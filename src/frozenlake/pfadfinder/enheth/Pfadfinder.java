package frozenlake.pfadfinder.enheth;

import frozenlake.Koordinate;
import frozenlake.Richtung;
import frozenlake.See;
import frozenlake.Zustand;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.core.transfer.Linear;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class Pfadfinder implements frozenlake.pfadfinder.IPfadfinder {

    //Bewertungsstruktur für das Lernen ohne neurales Netz
    double[][] seeBewertungen;

    //Perceptron für das Lernen mit neuronalem Netz
    MultiLayerPerceptron multiLayerPerceptron;

    //Lernrate
    double lernrate = 0.5;

    //Diskontfaktor
    double diskontfaktor = 0.85;

    //Abstrafung pro Schritt.
    //Das stellt sicher, dass wir beim Ansatz mit der Datenstruktur, nicht zum Feld zurückgehen, von dem wir gekommen sind
    double rewardSchritt = -1;

    //Ins Wasser zu fallen wird abgestraft
    double rewardWasser = -10;

    //Am Ziel anzukommen wird belohnt
    double rewardZiel = 10;

    //Variablen zum aktuellen Zustand des Lernes / Ablaufen des Sees
    See aktuellerSee;
    Koordinate aktuelleSpielerPosition;
    boolean stateValue;
    boolean neuralesNetz;
    boolean onPolicy;

    /**
     * Gibt Namen unseres Pfadfinders zurück
     * @return Name
     */
    @Override
    public String meinName() {
        return "enheth";
    }

    /** Startet das bestärkende Lernen für einen See. Am Ende der jeweiligen Einzelmethoden für die verschiedenen Aufgaben
     * wird die Datenstruktur bzw. das neuronale Netz gespeichert, um in der "starteUeberquerung" Methode wieder geladen
     * werden zu können
     * @param see Zu überquerender See
     * @param stateValue False: QValues verwenden, True: StateValue verwenden
     * @param neuronalesNetz True, wenn ein neuronales Netz verwendet wird
     * @param onPolicy False: Zufällige Züge beim Lernen, true: onPolicy-Lernen
     * @return true, wenn Lernvorgang erfolgreich war
     */
    @Override
    public boolean lerneSee(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {

        aktuelleSpielerPosition = see.spielerPosition();
        aktuellerSee = see;

        System.out.println("Starte das Lernen für den See " + see.getId() +
                " mit den Parametern statueValue = " + stateValue + ", neuronalesNetz = " + neuronalesNetz + " und onPolicy = " + onPolicy + "!" );

        //Aufgabe a
        if (stateValue && !neuronalesNetz) {

            //Initialisierung der Bewertungsstruktur. Der Status der Eisfelder wird nach Zeile und Spalte gespeichert.
            seeBewertungen = new double[see.getGroesse()][see.getGroesse()];

            if (onPolicy) {
                //On-Policy
                lerneSeeStateValueOnPolicy(see);
                System.out.println("Lernen für den See " + see.getId() + " abgeschlossen!");
                return true;
            } else {
                //Off-Policy
                lerneSeeStateValueOffPolicy(see);
                System.out.println("Lernen für den See " + see.getId() + " abgeschlossen!");
                return true;
            }

        }
        //Aufgabe b
        else if (stateValue && neuronalesNetz) {

            lerneSeeStateValueNeuronalesNetz(see);
            System.out.println("Lernen für den See " + see.getId() + " abgeschlossen!");
            return true;

        }

        return false;
    }

    /** Startet den Überquerungsvorgang für einen See. Wir Laden die Datenstruktur bzw. das neuronale Netz zum See
     * @param see Zu überquerender See
     * @param stateValue False: QValues verwenden, True: StateValue verwenden
     * @param neuronalesNetz True, wenn ein neuronales Netz verwendet wird
     * @param onPolicy False: Zufällige Züge beim Lernen, true: onPolicy-Lernen
     * @return true, wenn der Überquerungsvorgang erfolgreich initialisiert wurde.
     */
    @Override
    public boolean starteUeberquerung(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {

        //See und seine Konfiguration zwischenspeichern um in "naechsterSchritt" richtig vorgehen zu können
        this.stateValue = stateValue;
        this.neuralesNetz = neuronalesNetz;
        this.onPolicy = onPolicy;
        this.aktuelleSpielerPosition = see.spielerPosition();
        this.aktuellerSee = see;

        //Aufgabe a
        if (stateValue && !neuronalesNetz) {

            //Array mit State Values per ObjectInputStream laden
            if (onPolicy) {
                //On-Policy
                seeBewertungen = ladeArray(see,"ONPOLICY");
                int i = 0;
            } else {
                //Off-Policy
                seeBewertungen = ladeArray(see,"OFFPOLICY");
            }

        }
        //Aufgabe b
        else if (stateValue && neuronalesNetz) {

            //Neuronales Netz mit Neuroph Funktion laden
            multiLayerPerceptron = (MultiLayerPerceptron) MultiLayerPerceptron.createFromFile("gespeicherteNetze/" + see.getId() + ".nnet");

        }

        return false;
    }

    /** Wird wiederholt nach Start der Überquerung aufgerufen und muss den jeweils
     * nächsten Schritt liefern.
     * @param ausgangszustand: Gibt an, was sich auf dem aktuellen Feld befindet. Kann
     * nur "Start" oder "Eis" sein.
     * @return Richtung des nächsten Schrittes
     */
    @Override
    public Richtung naechsterSchritt(Zustand ausgangszustand) {
        Koordinate bestesFeld = null;
        //Aufgabe A
        if (stateValue && !neuralesNetz) {

            //suche besten Nachbarn in Datenstruktur
            bestesFeld = sucheBesterNachbar(aktuelleSpielerPosition);

        }
        //Aufgabe b
        else if (stateValue && neuralesNetz) {

            //suche besten Nachbarn in neuronalem Netz
            bestesFeld = sucheBesterNachbarNN(aktuelleSpielerPosition);

        }
        //Nicht implementierte Konfiguration
        else {
            return null;
        }

        //Gehe in die passende Richtung
        if (bestesFeld.getZeile() < aktuelleSpielerPosition.getZeile())
            return Richtung.HOCH;
        else if (bestesFeld.getZeile() > aktuelleSpielerPosition.getZeile())
            return Richtung.RUNTER;
        else if (bestesFeld.getSpalte() > aktuelleSpielerPosition.getSpalte())
            return Richtung.RECHTS;
        else if (bestesFeld.getSpalte() < aktuelleSpielerPosition.getSpalte())
            return Richtung.LINKS;

        return null;
    }

    /** Wird aufgerufen, wenn das Ziel erreicht wurde (endzustand = Ziel) oder wenn
     * der IPfadfinder ins Wasster gefallen ist (endzustand = Wasser). Hier muss aber nichts mehr konfiguriert werden,
     * da die Klasse beim nächsten Aufruf von "starteUeberquerung" wieder passend konfiguriert wird.
     * @param endzustand
     */
    @Override
    public void versuchZuende(Zustand endzustand) {
        System.out.println("Schade");
    }

    /**
     * Methode zum Lernen des Sees mit State Value Funktion, OnPolicy Strategie und ohne neuronales Netz
     * @param see Der aktuelle See
     */
    private void lerneSeeStateValueOnPolicy(See see) {

        //Mehrere Lerndurchgänge
        for (int durchgang = 0; durchgang < 10000; durchgang++) {

            Koordinate aktuellePosition = see.spielerPosition();
            Koordinate besterNachfolger = null;

            while (true) {

                //aktuelle Position ist nun bester Nachbar (OnPolicy)
                aktuellePosition = sucheBesterNachbar(aktuellePosition);

                if (aktuellePosition == null)
                    break;

                //bester Nachfolger der aktuellen Position bestimmen, um die Bewertung anpassen zu können
                besterNachfolger = sucheBesterNachbar(aktuellePosition);

                Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

                if (aktuellerZustand == Zustand.Ziel) {
                    //Wenn Ziel Bewertung auf RewardZiel und Durchgang vorbei
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
                    break;
                } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                    //Wenn Wasser Bewertung auf RewardWasser und Durchgang vorbei
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
                    break;
                } else
                    //Ansonsten anwenden der State Value Funktion zum bestimmen der neuen State Value an der Position
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1 - lernrate) * seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] + diskontfaktor * lernrate * (seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()] + rewardSchritt);

            }
        }

        //State Value Array speichern
        speichereArray(see, "ONPOLICY");

    }

    /**
     * Methode zum Finden des besten Nachbars an einer Position für Lernansätze mit Datenstruktur (also ohne neuronales Netz)
     * @param aktuelleKoordinate Koordinate für die der beste Nachbar gesucht wird
     * @return Koordinate des besten Nachbarn
     */
    private Koordinate sucheBesterNachbar(Koordinate aktuelleKoordinate) {

        //Beste Nachbarbewertung mit minimalem Wert initialisieren
        double besteBewertung = -Double.MAX_VALUE;
        Koordinate besteKoordinate = null;

        //Definieren aller Nachbarfelder
        Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() - 1);
        Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() + 1);
        Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile() + 1, aktuelleKoordinate.getSpalte());
        Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile() - 1, aktuelleKoordinate.getSpalte());

        //Wenn linkes Feld existiert und besser als besteBewertung
        if (linkesFeld.getSpalte() >= 0 && seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()];
            besteKoordinate = linkesFeld;
        }

        //Wenn rechtes Feld existiert und besser als besteBewertung
        if (rechtesFeld.getSpalte() < seeBewertungen.length && seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()];
            besteKoordinate = rechtesFeld;
        }

        //Wenn unteres Feld existiert und besser als besteBewertung
        if (unteresFeld.getZeile() < seeBewertungen.length && seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()];
            besteKoordinate = unteresFeld;
        }

        //Wenn oberes Feld existiert und besser als besteBewertung
        if (oberesFeld.getZeile() >= 0 && seeBewertungen[oberesFeld.getZeile()][oberesFeld.getSpalte()] >= besteBewertung) {
            besteKoordinate = oberesFeld;
        }

        return besteKoordinate;
    }

    /**
     * Methode zum Lernen des Sees mit State Value Funktion, OffPolicy Strategie und ohne neuronales Netz
     * @param see Der aktuelle See
     */
    private void lerneSeeStateValueOffPolicy(See see) {

        //Mehrere Lerndurchgänge
        for (int durchgang = 0; durchgang < 10000000; durchgang++) {

            Koordinate aktuellePosition = see.spielerPosition();
            Koordinate besterNachfolger = null;

            while (true) {

                //aktuelle Position ist nun zufälliger Nachbar (OffPolicy)
                aktuellePosition = sucheZufallsNachfolger(aktuellePosition);

                if (aktuellePosition == null)
                    break;

                //bester Nachfolger der aktuellen Position bestimmen, um die Bewertung anpassen zu können
                besterNachfolger = sucheBesterNachbar(aktuellePosition);

                Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

                if (aktuellerZustand == Zustand.Ziel) {
                    //Wenn Ziel Bewertung auf RewardZiel und Durchgang vorbei
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
                    break;
                } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                    //Wenn Wasser Bewertung auf RewardWasser und Durchgang vorbei
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
                    break;
                } else
                    //Ansonsten anwenden der State Value Funktion zum bestimmen der neuen State Value an der Position
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1 - lernrate) * seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] + diskontfaktor * lernrate * (seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()] + rewardSchritt);

            }
        }

        //State Value Array speichern
        speichereArray(see, "OFFPOLICY");

    }

    /**
     * Sucht zufälligen Nachbarn für OffPolicy ansatz
     * @param aktuelleKoordinate Aktuelle Koordinate für die Nachbar gesucht wird
     * @return Zufälliger Nachbar
     */
    private Koordinate sucheZufallsNachfolger(Koordinate aktuelleKoordinate) {

        Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() - 1);
        Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() + 1);
        Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile() + 1, aktuelleKoordinate.getSpalte());
        Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile() - 1, aktuelleKoordinate.getSpalte());

        int zufallsFeld = ThreadLocalRandom.current().nextInt(1, 5);
        switch (zufallsFeld) {
            case 1:
                if (ueberpruefeFeld(linkesFeld))
                    return linkesFeld;
                break;
            case 2:
                if (ueberpruefeFeld(rechtesFeld))
                    return rechtesFeld;
                break;
            case 3:
                if (ueberpruefeFeld(unteresFeld))
                    return unteresFeld;
                break;
            case 4:
                if (ueberpruefeFeld(oberesFeld))
                    return oberesFeld;
        }

        return null;
    }

    /**
     * überprüft ob Koordinate im Eisfeld liegt
     * @param k Koordinate
     * @return Koordinate liegt im Eisfeld
     */
    private boolean ueberpruefeFeld(Koordinate k) {
        return k.getSpalte() < seeBewertungen.length && k.getSpalte() >= 0 && k.getZeile() < seeBewertungen.length && k.getZeile() >= 0;
    }

    /**
     *  Methode zum Lernen des Sees mit State Value Funktion, OnPolicy Strategie und mit neuronalem Netz
     * @param see Der zu lernende See
     */
    private void lerneSeeStateValueNeuronalesNetz(See see) {

        //Perceptron erstellen und konfigurieren
        multiLayerPerceptron = new MultiLayerPerceptron(TransferFunctionType.TANH, see.getGroesse() + see.getGroesse(), see.getGroesse() + see.getGroesse(), 1);
        multiLayerPerceptron.getOutputNeurons().get(0).setTransferFunction(new Linear());
        multiLayerPerceptron.getLearningRule().setMaxError(0.01d);

        //100 Durchgänge
        for (int episode = 0; episode < 100; episode++) {

            //Erstellen eines neuen TrainingsSet
            DataSet trainingSet = new DataSet(see.getGroesse() + see.getGroesse(), 1);

            //Besuchte Koordinaten speichern, damit wir nicht die Felder besuchen auf denen wir in dieser Episode schon waren
            ArrayList<Koordinate> besuchteKoordinaten = new ArrayList<>();

            //Aktuelle Position und Zustand
            Koordinate aktuellePosition = see.spielerPosition();
            Zustand aktuellerZustand = see.zustandAn(aktuellePosition);


            while (true) {
                //Input Neuronen setzen, 0-9 -> Zeile, 10-19 -> Spalte
                double[] inputNeuronen = new double[see.getGroesse() + see.getGroesse()];
                inputNeuronen[aktuellePosition.getZeile()] = 1;
                inputNeuronen[aktuellePosition.getSpalte() + see.getGroesse()] = 1;

                besuchteKoordinaten.add(aktuellePosition);

                if (aktuellerZustand == Zustand.Ziel) {
                    //Wenn Ziel Bewertung auf Reward Ziel an der Position und Episode beenden

                    trainingSet.add(new DataSetRow(inputNeuronen, new double[]{rewardZiel}));
                    break;
                } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                    //Wenn Wasser Bewertung auf Reward Wasser an der Position und Episode beenden
                    trainingSet.add(new DataSetRow(inputNeuronen, new double[]{rewardWasser}));
                    break;
                } else {
                    //Ansonsten beste Koordinate suchen
                    Koordinate besteKoordinate = sucheBesterNachbarNN(aktuellePosition, besuchteKoordinaten);

                    if (besteKoordinate == null)
                        break;

                    //Bewertung des aktuellen Felds aus neuronalem Netz auslesen
                    double[] inputAktuellePosition = new double[see.getGroesse() + see.getGroesse()];
                    inputAktuellePosition[aktuellePosition.getZeile()] = 1;
                    inputAktuellePosition[see.getGroesse() + aktuellePosition.getSpalte()] = 1;
                    multiLayerPerceptron.setInput(inputAktuellePosition);
                    multiLayerPerceptron.calculate();
                    double aktuelleBewertung = multiLayerPerceptron.getOutput()[0];

                    //Bewertung des besten Nachfolgers aus neuronalem Netz auslesen
                    double[] inputBesterNachfolger = new double[see.getGroesse() + see.getGroesse()];
                    inputBesterNachfolger[besteKoordinate.getZeile()] = 1;
                    inputBesterNachfolger[see.getGroesse() + besteKoordinate.getSpalte()] = 1;
                    multiLayerPerceptron.setInput(inputBesterNachfolger);
                    multiLayerPerceptron.calculate();
                    double besteBewertung = multiLayerPerceptron.getOutput()[0];

                    //Trainingsset hinzufügen
                    trainingSet.add(new DataSetRow(inputNeuronen, new double[]{(1 - lernrate) * aktuelleBewertung + diskontfaktor * lernrate * (besteBewertung + rewardSchritt)}));

                    //Aktuelle Position und Koordinate aktualisieren
                    aktuellePosition = besteKoordinate;
                    aktuellerZustand = see.zustandAn(aktuellePosition);
                }
            }

            //Für alle Felder bisherige Bewertung aus den neuronalen Netz holen
            double[][] bisherigeBewertungen = new double[see.getGroesse()][see.getGroesse()];

            for (int zeile = 0; zeile < see.getGroesse(); zeile++) {
                for (int spalte = 0; spalte < see.getGroesse(); spalte++) {

                    double[] tmp = new double[see.getGroesse() + see.getGroesse()];
                    tmp[zeile] = 1;
                    tmp[spalte + see.getGroesse()] = 1;

                    multiLayerPerceptron.setInput(tmp);
                    multiLayerPerceptron.calculate();
                    bisherigeBewertungen[zeile][spalte] = multiLayerPerceptron.getOutput()[0];

                }
            }

            //Für alle Felder, die in dieser Episode NICHT besucht wurden, den alten Wert in das Trainingsset übergeben, damit dieser gleich bleibt
            for (int zeile = 0; zeile < see.getGroesse(); zeile++) {
                for (int spalte = 0; spalte < see.getGroesse(); spalte++) {

                    Koordinate aktuelleKoordinate = new Koordinate(zeile, spalte);

                    //Wenn Koordinate schon im Trainingsseit, weil sie in dieser Episode besucht wurde -> Continue
                    if (!koordinateNochNichtBenutzt(aktuelleKoordinate, besuchteKoordinaten))
                        continue;


                    double[] aktuellerInput = new double[see.getGroesse() + see.getGroesse()];
                    aktuellerInput[zeile] = 1;
                    aktuellerInput[spalte + see.getGroesse()] = 1;
                    trainingSet.add(aktuellerInput, new double[]{bisherigeBewertungen[zeile][spalte]});

                }

            }

            //Netz lernen
            multiLayerPerceptron.learn(trainingSet);
        }

        //Netz speichern
        multiLayerPerceptron.save("gespeicherteNetze/" + see.getId() + ".nnet");
    }


    /**
     * guckt ob die aktuelle Koordinate in einem Array von Koordinaten enthalten ist
     * @param aktuelleKoordinate Koordinate, die überprüft wird
     * @param besuchteKoordinaten Array von Koordinaten
     * @return Koordinate nicht im Array
     */
    private boolean koordinateNochNichtBenutzt(Koordinate aktuelleKoordinate, ArrayList<Koordinate> besuchteKoordinaten) {

        for (Koordinate k : besuchteKoordinaten) {

            if (aktuelleKoordinate.getSpalte() == k.getSpalte() && aktuelleKoordinate.getZeile() == k.getZeile())
                return false;
        }

        return true;
    }

    /**
     * sucht besten Nachbarn ohne besuchte Felder auszuschließen
     * @param aktuelleKoordinate Aktuelle Koordinate
     * @return bester Nachbar
     */
    private Koordinate sucheBesterNachbarNN(Koordinate aktuelleKoordinate) {
        ArrayList<Koordinate> empty = new ArrayList<>();
        return sucheBesterNachbarNN(aktuelleKoordinate, empty);
    }

    /**
     *
     * @param aktuelleKoordinate Aktuelle Koordinate
     * @param besuchteKoordinaten Liste der besuchten Koordinaten
     * @return bester Nachbar
     */
    private Koordinate sucheBesterNachbarNN(Koordinate aktuelleKoordinate, ArrayList<Koordinate> besuchteKoordinaten) {
        //Beste Nachbarbewertung mit minimalem Wert initialisieren
        double besteBewertung = -Double.MAX_VALUE;
        Koordinate besteKoordinate = null;

        //Alle Nachbarfelder definieren
        Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() - 1);
        Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() + 1);
        Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile() + 1, aktuelleKoordinate.getSpalte());
        Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile() - 1, aktuelleKoordinate.getSpalte());

        //input Neuronen für die Felder definieren
        double[] inputNeuronen = new double[aktuellerSee.getGroesse() + aktuellerSee.getGroesse()];

        //Wenn linkes Feld existiert und Koordinate nicht in besuchteKoordinaten
        if (linkesFeld.getSpalte() >= 0 && koordinateNochNichtBenutzt(linkesFeld, besuchteKoordinaten)) {
            inputNeuronen[linkesFeld.getZeile()] = 1;
            inputNeuronen[aktuellerSee.getGroesse() + linkesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();

            //Bewertung aus dem neuronalen Netz auslesen
            double bewertungLinkesFeld = multiLayerPerceptron.getOutput()[0];

            if (bewertungLinkesFeld > besteBewertung) {
                besteKoordinate = linkesFeld;
                besteBewertung = bewertungLinkesFeld;
            }
        }

        //Input Neuronen für nächstes Feld resetten
        Arrays.fill(inputNeuronen, 0);

        //Wenn rechtes Feld existiert und Koordinate nicht in besuchteKoordinaten
        if (rechtesFeld.getSpalte() < aktuellerSee.getGroesse() && koordinateNochNichtBenutzt(rechtesFeld, besuchteKoordinaten)) {
            inputNeuronen[rechtesFeld.getZeile()] = 1;
            inputNeuronen[aktuellerSee.getGroesse() + rechtesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();

            //Bewertung aus dem neuronalen Netz auslesen
            double bewertungRechtesFeld = multiLayerPerceptron.getOutput()[0];

            if (bewertungRechtesFeld > besteBewertung) {
                besteKoordinate = rechtesFeld;
                besteBewertung = bewertungRechtesFeld;
            }
        }

        //Input Neuronen für nächstes Feld resetten
        Arrays.fill(inputNeuronen, 0);

        //Wenn unteres Feld existiert und Koordinate nicht in besuchteKoordinaten
        if (unteresFeld.getZeile() < aktuellerSee.getGroesse() && koordinateNochNichtBenutzt(unteresFeld, besuchteKoordinaten)) {
            inputNeuronen[unteresFeld.getZeile()] = 1;
            inputNeuronen[aktuellerSee.getGroesse() + unteresFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();

            //Bewertung aus dem neuronalen Netz auslesen
            double bewertungUnteresFeld = multiLayerPerceptron.getOutput()[0];

            if (bewertungUnteresFeld > besteBewertung) {
                besteKoordinate = unteresFeld;
                besteBewertung = bewertungUnteresFeld;
            }
        }

        //Input Neuronen für nächstes Feld resetten
        Arrays.fill(inputNeuronen, 0);

        //Wenn oberes Feld existiert und Koordinate nicht in besuchteKoordinaten
        if (oberesFeld.getZeile() >= 0 && koordinateNochNichtBenutzt(oberesFeld, besuchteKoordinaten)) {
            inputNeuronen[oberesFeld.getZeile()] = 1;
            inputNeuronen[aktuellerSee.getGroesse() + oberesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();

            //Bewertung aus dem neuronalen Netz auslesen
            double bewertungOberesFeld = multiLayerPerceptron.getOutput()[0];

            if (bewertungOberesFeld > besteBewertung) {
                besteKoordinate = oberesFeld;
                besteBewertung = bewertungOberesFeld;
            }
        }

        return besteKoordinate;
    }

    /**
     * Array speichern
     * @param see See für die ID
     * @param name Namensendung für Datei
     */
    private void speichereArray(See see, String name) {

        try {

            FileOutputStream fileOut = new FileOutputStream("gespeicherteArrays/" + see.getId() + "_" + name);
            ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
            objectOut.writeObject(seeBewertungen);
            objectOut.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    /**
     * Array laden
     * @param see See für die ID
     * @param name Namensendung für Datei
     * @return Array mit Datenstruktur
     */
    private double[][] ladeArray(See see, String name) {

        try {

            FileInputStream fileIn = new FileInputStream("gespeicherteArrays/" + see.getId() + "_" + name);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);

            double[][] obj = (double[][]) objectIn.readObject();

            objectIn.close();
            return obj;

        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

    }
}
