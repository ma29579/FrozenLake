package frozenlake;

import frozenlake.pfadfinder.IPfadfinder;
import frozenlake.pfadfinder.enheth.Pfadfinder;

public class SeeSimulator {

    public static void main(String[] args) {
        int anzahlSchritte = 0;
        try {


            See testsee = new See("Testsee", 6, new Koordinate(0, 0), new Koordinate(5, 5));
            testsee.wegErzeugen();
            testsee.speichereSee("Testsee");

            Pfadfinder test = new Pfadfinder();
            test.lerneSee(testsee, true, true, true);

            test.starteUeberquerung(testsee, true,true,true);
            testsee.anzeigen();

            Zustand naechsterZustand = Zustand.Start;
            do {
                Richtung r = test.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = testsee.geheNach(r);
                anzahlSchritte++;
                testsee.anzeigen();
            } while (!((naechsterZustand == Zustand.Ziel) || (naechsterZustand == Zustand.Wasser)));

            if (naechsterZustand == Zustand.Ziel) {
                System.out.println("Sie haben Ihr Ziel erreicht! Anzahl Schritte: " + anzahlSchritte);
            } else {
                System.out.println("Sie sind im Wasser gelandet. Anzahl Schritte bis dahin: " + anzahlSchritte);
            }
        } catch (Exception ex) {
            System.err.println("Exception nach " + anzahlSchritte + " Schritten!");
            ex.printStackTrace();
        }
    }
}
