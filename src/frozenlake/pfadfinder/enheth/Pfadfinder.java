package frozenlake.pfadfinder.enheth;

import frozenlake.Koordinate;
import frozenlake.Richtung;
import frozenlake.See;
import frozenlake.Zustand;
import org.neuroph.core.data.DataSet;
import org.neuroph.core.transfer.Linear;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.util.TransferFunctionType;

import java.util.*;
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

            multiLayerPerceptron = new MultiLayerPerceptron(TransferFunctionType.TANH, see.getGroesse()*see.getGroesse(), see.getGroesse()*see.getGroesse(), 1);
            multiLayerPerceptron.getOutputNeurons()[0].setTransferFunction(new Linear());
            multiLayerPerceptron.getLearningRule().setMaxError(0.01d);

            for(int episode = 0; episode < 10; episode++){

                DataSet trainingSet = new DataSet(see.getGroesse()*see.getGroesse(),1);
                ArrayList<Koordinate> besuchteKoordinaten = new ArrayList<>();
                Koordinate aktuellePosition = see.spielerPosition();
                Zustand aktuellerZustand = see.zustandAn(aktuellePosition);


                while(true){
                    double reward = 0;
                    double[] inputNeuronen = new double[see.getGroesse()*see.getGroesse()];
                    besuchteKoordinaten.add(aktuellePosition);

                    if (aktuellerZustand == Zustand.Ziel) {
                        inputNeuronen[aktuellePosition.getZeile()+aktuellePosition.getSpalte()*see.getGroesse()] = 1;
                        trainingSet.addRow(inputNeuronen, new double[]{100});
                        break;
                    } else if (aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
                        inputNeuronen[aktuellePosition.getZeile()+aktuellePosition.getSpalte()*see.getGroesse()] = 1;
                        trainingSet.addRow(inputNeuronen, new double[]{-100});
                        break;
                    } else {

                        Koordinate besteKoordinate = berechneMaxNachfolgerNN(aktuellePosition,see, besuchteKoordinaten);

                        if(besteKoordinate == null)
                            break;

                        double[] inputBesterNachfolger = new double[see.getGroesse()*see.getGroesse()];
                        double[] inputAktuellePosition = new double[see.getGroesse()*see.getGroesse()];
                        inputAktuellePosition[aktuellePosition.getZeile()*see.getGroesse()+aktuellePosition.getSpalte()] = 1;

                        multiLayerPerceptron.setInput(inputAktuellePosition);
                        multiLayerPerceptron.calculate();
                        double aktuelleBewertung = multiLayerPerceptron.getOutput()[0];

                        inputBesterNachfolger[besteKoordinate.getZeile()*see.getGroesse()+besteKoordinate.getSpalte()] = 1;
                        multiLayerPerceptron.setInput(inputBesterNachfolger);
                        multiLayerPerceptron.calculate();
                        double besteBewertung = multiLayerPerceptron.getOutput()[0];

                        inputNeuronen[aktuellePosition.getZeile()+aktuellePosition.getSpalte()*see.getGroesse()] = 1;


                        double[] trainingData = new double[]{(1 - lernrate) * aktuelleBewertung + diskontfaktor * lernrate * (besteBewertung + rewardSchritt)};
                        trainingSet.addRow(inputNeuronen, new double[]{(1 - lernrate) * aktuelleBewertung + diskontfaktor * lernrate * (besteBewertung + rewardSchritt)});

                        aktuellePosition = besteKoordinate;
                        aktuellerZustand = see.zustandAn(aktuellePosition);
                    }
                }

                multiLayerPerceptron.learn(trainingSet);
            }


        }

        double[][] testBewertungen = new double[see.getGroesse()][see.getGroesse()];

        for(int zeile = 0; zeile < see.getGroesse(); zeile++){
            for(int spalte = 0; spalte < see.getGroesse(); spalte++){

                double[] tmp = new double[see.getGroesse()*see.getGroesse()];
                tmp[zeile*see.getGroesse()+spalte] = 1;

                multiLayerPerceptron.setInput(tmp);
                multiLayerPerceptron.calculate();
                testBewertungen[zeile][spalte] = multiLayerPerceptron.getOutput()[0];

            }
        }


        for (int i = 0; i < see.getGroesse(); i++) {
            for (int j = 0; j < see.getGroesse(); j++) {

                System.out.printf("%10.2f", testBewertungen[i][j]);
                System.out.print(" ");

            }

            System.out.println("");
        }

        return false;
    }

    private boolean koordinateNochNichtBenutzt(Koordinate aktuelleKoordinate, ArrayList<Koordinate> besuchteKoordinaten){

        for(Koordinate k : besuchteKoordinaten){

            if(aktuelleKoordinate.getSpalte() == k.getSpalte() && aktuelleKoordinate.getZeile() == k.getZeile())
                return false;
        }

        return true;
    }

    private Koordinate berechneMaxNachfolgerNN(Koordinate aktuelleKoordinate, See see, ArrayList<Koordinate> besuchteKoordinaten){

        double besteBewertung = -Double.MAX_VALUE;
        Koordinate besteKoordinate = null;
        Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() - 1);
        Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte() + 1);
        Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile() + 1, aktuelleKoordinate.getSpalte());
        Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile() - 1, aktuelleKoordinate.getSpalte());

        double[] inputNeuronen = new double[see.getGroesse()*see.getGroesse()];

        if (linkesFeld.getSpalte() >= 0 && koordinateNochNichtBenutzt(linkesFeld,besuchteKoordinaten)) {
            inputNeuronen[linkesFeld.getZeile()*see.getGroesse()+linkesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();
            double bewertungLinkesFeld = multiLayerPerceptron.getOutput()[0];

            if(bewertungLinkesFeld > besteBewertung) {
                besteKoordinate = linkesFeld;
                besteBewertung = bewertungLinkesFeld;
            }
        }

        Arrays.fill(inputNeuronen,0);

        if (rechtesFeld.getSpalte() < see.getGroesse() && koordinateNochNichtBenutzt(rechtesFeld,besuchteKoordinaten)) {
            inputNeuronen[rechtesFeld.getZeile()*see.getGroesse()+rechtesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();
            double bewertungRechtesFeld = multiLayerPerceptron.getOutput()[0];

            if(bewertungRechtesFeld > besteBewertung) {
                besteKoordinate = rechtesFeld;
                besteBewertung = bewertungRechtesFeld;
            }
        }

        Arrays.fill(inputNeuronen,0);

        if (unteresFeld.getZeile() < see.getGroesse() && koordinateNochNichtBenutzt(unteresFeld,besuchteKoordinaten)) {
            inputNeuronen[unteresFeld.getZeile()*see.getGroesse()+unteresFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();
            double bewertungUnteresFeld = multiLayerPerceptron.getOutput()[0];

            if(bewertungUnteresFeld > besteBewertung) {
                besteKoordinate = unteresFeld;
                besteBewertung = bewertungUnteresFeld;
            }
        }

        Arrays.fill(inputNeuronen,0);

        if (oberesFeld.getZeile() >= 0 && koordinateNochNichtBenutzt(oberesFeld,besuchteKoordinaten)) {
            inputNeuronen[oberesFeld.getZeile()*see.getGroesse()+oberesFeld.getSpalte()] = 1;
            multiLayerPerceptron.setInput(inputNeuronen);
            multiLayerPerceptron.calculate();
            double bewertungOberesFeld = multiLayerPerceptron.getOutput()[0];

            if(bewertungOberesFeld > besteBewertung) {
                besteKoordinate = oberesFeld;
                besteBewertung = bewertungOberesFeld;
            }
        }

        return besteKoordinate;
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
