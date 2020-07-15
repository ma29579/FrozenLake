package frozenlake;

import frozenlake.pfadfinder.enheth.Pfadfinder;

public class SeeSimulator {

    public static void main(String[] args) {
        int anzahlSchritte = 0;
        try {


            See testsee = new See("Testsee", 6, new Koordinate(0, 0), new Koordinate(5, 5));
            See offPolicySee = new See("offPolicySee", 6, new Koordinate(0,0), new Koordinate(5,5));
            See neuronalesNetzSee = new See("NeuronalesNetzSee", 6, new Koordinate(0,0), new Koordinate(5,5));
            testsee.wegErzeugen();
            testsee.speichereSee("testsee");
            offPolicySee.wegErzeugen();
            neuronalesNetzSee.wegErzeugen();
            neuronalesNetzSee.speichereSee("NNSEE");

            Pfadfinder test = new Pfadfinder();
            test.lerneSee(testsee, false, false, true);
            test.lerneSee(offPolicySee,true,false,false);
            test.lerneSee(neuronalesNetzSee,true,true,true);

            test.starteUeberquerung(testsee, false,false,true);
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

            System.out.println("--------------------------");

            test.starteUeberquerung(offPolicySee, true,false,false);
            offPolicySee.anzeigen();

            anzahlSchritte = 0;
            naechsterZustand = Zustand.Start;
            do {
                Richtung r = test.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = offPolicySee.geheNach(r);
                anzahlSchritte++;
                offPolicySee.anzeigen();
            } while (!((naechsterZustand == Zustand.Ziel) || (naechsterZustand == Zustand.Wasser)));

            if (naechsterZustand == Zustand.Ziel) {
                System.out.println("Sie haben Ihr Ziel erreicht! Anzahl Schritte: " + anzahlSchritte);
            } else {
                System.out.println("Sie sind im Wasser gelandet. Anzahl Schritte bis dahin: " + anzahlSchritte);
            }

            System.out.println("--------------------------");

            test.starteUeberquerung(neuronalesNetzSee, true,true,true);
            neuronalesNetzSee.anzeigen();

            anzahlSchritte = 0;
            naechsterZustand = Zustand.Start;
            do {
                Richtung r = test.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = neuronalesNetzSee.geheNach(r);
                anzahlSchritte++;
                neuronalesNetzSee.anzeigen();
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
