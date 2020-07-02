package frozenlake.pfadfinder.enheth;

import frozenlake.Koordinate;
import frozenlake.Richtung;
import frozenlake.See;
import frozenlake.Zustand;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Pfadfinder implements frozenlake.pfadfinder.IPfadfinder{

	double[][] seeBewertungen;
	double lernrate = 0.5;
	double diskontfaktor = 0.85;
	double rewardSchritt = -1;
	double rewardWasser = -100;
	double rewardZiel = 100;

	@Override
	public String meinName() {
		return "enheth";
	}

	@Override
	public boolean lerneSee(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {

		//Aufgabe a
		if(stateValue && !neuronalesNetz){

			//Initialisierung der Bewertungsstruktur
			seeBewertungen = new double[see.getGroesse()][see.getGroesse()];
			Koordinate position =  see.spielerPosition();

			for(int i = 0; i < see.getGroesse(); i++){
				for(int j = 0; j < see.getGroesse(); j++){
					seeBewertungen[i][j] = 0;
				}
			}

			if(onPolicy){
				//On-Policy

				for(int durchgang = 0; durchgang < 100; durchgang++){

					Koordinate aktuellePosition = see.spielerPosition();
					Koordinate besterNachfolger = null;

					while(true){

						aktuellePosition = sucheBestesFeld(aktuellePosition);
						besterNachfolger = sucheBestesFeld(aktuellePosition);

						double r;

						if(aktuellePosition == null)
							break;


						Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

						if(aktuellerZustand == Zustand.Ziel){
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
							break;
						}
						else if(aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
							break;
						}
						else
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1-lernrate)*seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()]+diskontfaktor*lernrate*(seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()]+rewardSchritt);

					}
				}

				seeAusgabe(see);

			} else {
				//Off-Policy

				for(int durchgang = 0; durchgang < 10000000; durchgang++){

					Koordinate aktuellePosition = see.spielerPosition();
					Koordinate besterNachfolger = null;

					while(true){

						aktuellePosition = sucheZufallsNachfolger(aktuellePosition);

						if(aktuellePosition == null)
							break;

						besterNachfolger = sucheBestesFeld(aktuellePosition);

						Zustand aktuellerZustand = see.zustandAn(aktuellePosition);

						if(aktuellerZustand == Zustand.Ziel){
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardZiel;
							break;
						}
						else if(aktuellerZustand == Zustand.UWasser || aktuellerZustand == Zustand.Wasser) {
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = rewardWasser;
							break;
						}
						else
							seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()] = (1-lernrate)*seeBewertungen[aktuellePosition.getZeile()][aktuellePosition.getSpalte()]+diskontfaktor*lernrate*(seeBewertungen[besterNachfolger.getZeile()][besterNachfolger.getSpalte()]+rewardSchritt);

					}
				}


				seeAusgabe(see);
			}

		}
		//Aufgabe b
		else if(stateValue && neuronalesNetz) {


			if(onPolicy){

			}

		}

		return false;
	}

	private void seeAusgabe(See see){

		for(int i=0; i < see.getGroesse(); i++){
			for(int j = 0; j < see.getGroesse(); j++){

					System.out.printf("%10.2f",seeBewertungen[i][j]);
					System.out.print(" ");

			}

			System.out.println("");
		}

	}

	private Koordinate sucheZufallsNachfolger(Koordinate aktuelleKoordinate){

		Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte()-1);
		Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte()+1);
		Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile()+1, aktuelleKoordinate.getSpalte());
		Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile()-1, aktuelleKoordinate.getSpalte());

		int zufallsFeld = ThreadLocalRandom.current().nextInt(1,5);
		switch (zufallsFeld){
			case 1:
				if(ueberpruefeFeld(linkesFeld))
					return linkesFeld;
				break;
			case 2:
				if(ueberpruefeFeld(rechtesFeld))
					return rechtesFeld;
				break;
			case 3:
				if(ueberpruefeFeld(unteresFeld))
					return unteresFeld;
				break;
			case 4:
				if(ueberpruefeFeld(oberesFeld))
					return oberesFeld;
		}

		return null;
	}

	private boolean ueberpruefeFeld(Koordinate k){

		if(k.getSpalte() < seeBewertungen.length && k.getSpalte() >= 0) {
			if (k.getZeile() < seeBewertungen.length && k.getZeile() >= 0)
				return true;
		}

		return false;
	}

	private Koordinate sucheBestesFeld(Koordinate aktuelleKoordinate){

		double besteBewertung = - Double.MAX_VALUE;
		Koordinate besteKoordinate = null;
		Koordinate linkesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte()-1);
		Koordinate rechtesFeld = new Koordinate(aktuelleKoordinate.getZeile(), aktuelleKoordinate.getSpalte()+1);
		Koordinate unteresFeld = new Koordinate(aktuelleKoordinate.getZeile()+1, aktuelleKoordinate.getSpalte());
		Koordinate oberesFeld = new Koordinate(aktuelleKoordinate.getZeile()-1, aktuelleKoordinate.getSpalte());

		if(linkesFeld.getSpalte() >= 0 && seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()] >= besteBewertung) {
			besteKoordinate = linkesFeld;
			besteBewertung = seeBewertungen[linkesFeld.getZeile()][linkesFeld.getSpalte()];
		}

		if(rechtesFeld.getSpalte() < seeBewertungen.length && seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()] >= besteBewertung){
			besteBewertung = seeBewertungen[rechtesFeld.getZeile()][rechtesFeld.getSpalte()];
			besteKoordinate = rechtesFeld;
		}

		if(unteresFeld.getZeile() < seeBewertungen.length && seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()] >= besteBewertung){
			besteBewertung = seeBewertungen[unteresFeld.getZeile()][unteresFeld.getSpalte()];
			besteKoordinate = unteresFeld;
		}

		if(oberesFeld.getZeile() >= 0 && seeBewertungen[oberesFeld.getZeile()][oberesFeld.getSpalte()] >= besteBewertung) {
			besteBewertung = seeBewertungen[oberesFeld.getZeile()][oberesFeld.getSpalte()];
			besteKoordinate = oberesFeld;
		}

		return besteKoordinate;
	}

	@Override
	public boolean starteUeberquerung(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {
		//TODO Hier sind Sie gefragt
		return false;
	}

	@Override
	public Richtung naechsterSchritt(Zustand ausgangszustand) {
		//TODO Hier sind Sie gefragt
		return null;
	}

	@Override
	public void versuchZuende(Zustand endzustand) {
		//TODO Hier sind Sie gefragt
	}
}
