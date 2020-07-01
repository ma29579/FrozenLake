package frozenlake.pfadfinder.enheth;

import frozenlake.Richtung;
import frozenlake.See;
import frozenlake.Zustand;

public class Pfadfinder implements frozenlake.pfadfinder.IPfadfinder{

	@Override
	public String meinName() {
		return "Mustergruppe";
	}

	@Override
	public boolean lerneSee(See see, boolean stateValue, boolean neuronalesNetz, boolean onPolicy) {
		//TODO Hier sind Sie gefragt
		return false;
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
