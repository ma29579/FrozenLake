package frozenlake.pfadfinder.enheth;

import frozenlake.Koordinate;
import frozenlake.Richtung;
import frozenlake.See;
import frozenlake.Zustand;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.data.DataSetRow;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Pfadfinder implements frozenlake.pfadfinder.IPfadfinder {

    double[][] seeBewertungen;
    double lernrate = 0.5;
    double diskontfaktor = 0.85;
    double rewardSchritt = -1;
    double rewardWasser = -100;
    double rewardZiel = 100;
    Koordinate aktuelleSpielerPosition;
    MultiLayerPerceptron multiLayerPerceptron;

    boolean stateValue;
    boolean neuralesNetz;
    boolean onPolicy;

    @Override
    public String meinName() {
        return "enheth";
    }

    @Override
    public boolean lerneSee(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {

        this.stateValue = stateValue;
        this.neuralesNetz = neuronalesNetz;
        this.onPolicy = onPolicy;

        aktuelleSpielerPosition = see.spielerPosition();

        //Aufgabe a
        if (stateValue && !neuronalesNetz) {

            //Initialisierung der Bewertungsstruktur
            seeBewertungen = new double[see.getGroesse()][see.getGroesse()];
            Koordinate position = see.spielerPosition();

            for (int i = 0; i < see.getGroesse(); i++) {
                for (int j = 0; j < see.getGroesse(); j++) {
                    seeBewertungen[i][j] = 0;
                }
            }

            if (onPolicy) {
                //On-Policy
                lerneSeeStateValueOnPolicy(see);
            } else {
                //Off-Policy
				lerneSeeStateValueOffPolicy(see);
            }

        }
        //Aufgabe b
        else if (stateValue && neuronalesNetz) {

            DataSet trainingSet = new DataSet(see.getGroesse()*see.getGroesse(),1);

            Koordinate aktuellePosition = see.spielerPosition();
            Koordinate besterNachfolger = null;

            do{

                aktuellePosition = sucheBestesFeld(aktuellePosition);
                besterNachfolger = sucheBestesFeld(aktuellePosition);

                double r;

                if (aktuellePosition == null)
                    break;

                double vsResult = 0;

                Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

                if (aktuellerZustand == Zustand.Ziel) {
                    vsResult = rewardZiel;
                    break;
                } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                    vsResult = rewardWasser;
                    break;
                } else
                    vsResult = (1 - lernrate) * seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] + diskontfaktor * lernrate * (seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()] + rewardSchritt);


                double[] zustand = new double[see.getGroesse()*see.getGroesse()];
                for(int zeile = 0; zeile < see.getGroesse(); zeile++){
                    for(int spalte = 0; spalte < see.getGroesse(); spalte++){

                        if(aktuellePosition.getSpalte() == spalte && aktuellePosition.getZeile() == zeile)
                            zustand[zeile*see.getGroesse()+spalte] = 1;
                        else
                            zustand[zeile*see.getGroesse()+spalte] = 0;
                    }
                }
                trainingSet.addRow(new DataSetRow(zustand,new double[]{vsResult}));

            }while (true);


            for(int zeile = 0; zeile < see.getGroesse(); zeile++){
                for(int spalte = 0; spalte < see.getGroesse(); spalte++){


                }
            }
            multiLayerPerceptron = new MultiLayerPerceptron(TransferFunctionType.TANH, see.getGroesse()*see.getGroesse(), see.getGroesse()*see.getGroesse(), 1);
            multiLayerPerceptron.getLearningRule().setMaxError(0.00000001d);
            multiLayerPerceptron.learn(trainingSet);


        }

        return false;
    }

    private double berechneMaxNachfolgerNN(int zustand){

        return 0;
    }

    private void lerneSeeStateValueOffPolicy(See see){

		for (int durchgang = 0; durchgang < 10000000; durchgang++) {

			Koordinate aktuellePosition = see.spielerPosition();
			Koordinate besterNachfolger = null;

			while (true) {

				aktuellePosition = sucheZufallsNachfolger(aktuellePosition);

				if (aktuellePosition == null)
					break;

				besterNachfolger = sucheBestesFeld(aktuellePosition);

				Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

				if (aktuellerZustand == Zustand.Ziel) {
					seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
					break;
				} else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
					seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
					break;
				} else
					seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1 - lernrate) * seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] + diskontfaktor * lernrate * (seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()] + rewardSchritt);

			}
		}

	}

    private void lerneSeeStateValueOnPolicy(See see) {

        for (int durchgang = 0; durchgang < 10; durchgang++) {

            Koordinate aktuellePosition = see.spielerPosition();
            Koordinate besterNachfolger = null;

            while (true) {

                aktuellePosition = sucheBestesFeld(aktuellePosition);
                besterNachfolger = sucheBestesFeld(aktuellePosition);

                double r;

                if (aktuellePosition == null)
                    break;


                Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

                if (aktuellerZustand == Zustand.Ziel) {
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
                    break;
                } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
                    break;
                } else
                    seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1 - lernrate) * seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] + diskontfaktor * lernrate * (seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()] + rewardSchritt);

            }
        }

    }

    private void seeAusgabe(See see) {

        for (int i = 0; i < see.getGroesse(); i++) {
            for (int j = 0; j < see.getGroesse(); j++) {

                System.out.printf("%10.2f", seeBewertungen[i][j]);
                System.out.print(" ");

            }

            System.out.println("");
        }

    }

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

    private boolean ueberpruefeFeld(Koordinate k) {

        if (k.getSpalte() < seeBewertungen.length && k.getSpalte() >= 0) {
            if (k.getZeile() < seeBewertungen.length && k.getZeile() >= 0)
                return true;
        }

        return false;
    }

    private Koordinate sucheBestesFeld(Koordinate aktuelleKoordinate) {

        double besteBewertung = -Double.MAX_VALUE;
        Koordinate besteKoordinate = null;
        Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() - 1);
        Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() + 1);
        Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile() + 1, aktuelleKoordinate.getSpalte());
        Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile() - 1, aktuelleKoordinate.getSpalte());

        if (linkesFeld.getSpalte() >= 0 && seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()] >= besteBewertung) {
            besteKoordinate = linkesFeld;
            besteBewertung = seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()];
        }

        if (rechtesFeld.getSpalte() < seeBewertungen.length && seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()];
            besteKoordinate = rechtesFeld;
        }

        if (unteresFeld.getZeile() < seeBewertungen.length && seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()];
            besteKoordinate = unteresFeld;
        }

        if (oberesFeld.getZeile() >= 0 && seeBewertungen[oberesFeld.getZeile()][oberesFeld.getSpalte()] >= besteBewertung) {
            besteBewertung = seeBewertungen[oberesFeld.getZeile()][oberesFeld.getSpalte()];
            besteKoordinate = oberesFeld;
        }

        return besteKoordinate;
    }

    @Override
    public boolean starteUeberquerung(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {

        aktuelleSpielerPosition = see.spielerPosition();

        //Aufgabe a
        if (stateValue && !neuronalesNetz) {


            if (onPolicy) {
                //On-Policy
                lerneSeeStateValueOnPolicy(see);
            } else {
                //Off-Policy
                lerneSeeStateValueOffPolicy(see);
            }

        }
        //Aufgabe b
        else if (stateValue && neuronalesNetz) {


            if (onPolicy) {

            }

        }

        return false;
    }

    @Override
    public Richtung naechsterSchritt(Zustand ausgangszustand) {

        if(stateValue && !neuralesNetz){

           Koordinate bestesFeld = sucheBestesFeld(aktuelleSpielerPosition);

           if(bestesFeld.getZeile() < aktuelleSpielerPosition.getZeile())
               return Richtung.HOCH;
           else if(bestesFeld.getZeile() > aktuelleSpielerPosition.getZeile())
               return Richtung.RUNTER;
           else if(bestesFeld.getSpalte() > aktuelleSpielerPosition.getSpalte())
               return Richtung.RECHTS;
           else if(bestesFeld.getSpalte() < aktuelleSpielerPosition.getSpalte())
               return Richtung.LINKS;

        }

        return null;
    }

    @Override
    public void versuchZuende(Zustand endzustand) {
        System.out.println("Schade");
    }
}
