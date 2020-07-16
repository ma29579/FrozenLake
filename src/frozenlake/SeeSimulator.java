package frozenlake;

import frozenlake.pfadfinder.enheth.Pfadfinder;

public class SeeSimulator {

    public static void main(String[] args) {
        int anzahlSchritte = 0;
        try {


            See stateValueOnPolicySee = new See("stateValueOnPolicySee", 6, new Koordinate(0, 0), new Koordinate(5, 5));
            See stateValueOffPolicySee = new See("stateValueOffPolicySee", 6, new Koordinate(0,0), new Koordinate(5,5));
            See neuronalesNetzSee = new See("NeuronalesNetzSee", 6, new Koordinate(0,0), new Koordinate(5,5));
            See qLearningSee = new See("qLearningSee",6,new Koordinate(0,0), new Koordinate(5,5));

            stateValueOnPolicySee.wegErzeugen();
            stateValueOffPolicySee.wegErzeugen();
            neuronalesNetzSee.wegErzeugen();
            qLearningSee.wegErzeugen();

            Pfadfinder pfadfinder_enheth = new Pfadfinder();

            pfadfinder_enheth.lerneSee(stateValueOnPolicySee, true, false, true);
            pfadfinder_enheth.lerneSee(stateValueOffPolicySee,true,false,false);
            pfadfinder_enheth.lerneSee(neuronalesNetzSee,true,true,true);
            pfadfinder_enheth.lerneSee(qLearningSee,false,false,false);

            pfadfinder_enheth.starteUeberquerung(stateValueOnPolicySee, true,false,true);
            stateValueOnPolicySee.anzeigen();

            Zustand naechsterZustand = Zustand.Start;
            do {
                Richtung r = pfadfinder_enheth.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = stateValueOnPolicySee.geheNach(r);
                anzahlSchritte++;
                stateValueOnPolicySee.anzeigen();
            } while (!((naechsterZustand == Zustand.Ziel) || (naechsterZustand == Zustand.Wasser)));

            if (naechsterZustand == Zustand.Ziel) {
                System.out.println("Sie haben Ihr Ziel erreicht! Anzahl Schritte: " + anzahlSchritte);
            } else {
                System.out.println("Sie sind im Wasser gelandet. Anzahl Schritte bis dahin: " + anzahlSchritte);
            }

            System.out.println("--------------------------");

            pfadfinder_enheth.starteUeberquerung(stateValueOffPolicySee, true,false,false);
            stateValueOffPolicySee.anzeigen();

            anzahlSchritte = 0;
            naechsterZustand = Zustand.Start;
            do {
                Richtung r = pfadfinder_enheth.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = stateValueOffPolicySee.geheNach(r);
                anzahlSchritte++;
                stateValueOffPolicySee.anzeigen();
            } while (!((naechsterZustand == Zustand.Ziel) || (naechsterZustand == Zustand.Wasser)));

            if (naechsterZustand == Zustand.Ziel) {
                System.out.println("Sie haben Ihr Ziel erreicht! Anzahl Schritte: " + anzahlSchritte);
            } else {
                System.out.println("Sie sind im Wasser gelandet. Anzahl Schritte bis dahin: " + anzahlSchritte);
            }

            System.out.println("--------------------------");

            pfadfinder_enheth.starteUeberquerung(neuronalesNetzSee, true,true,true);
            neuronalesNetzSee.anzeigen();

            anzahlSchritte = 0;
            naechsterZustand = Zustand.Start;
            do {
                Richtung r = pfadfinder_enheth.naechsterSchritt(naechsterZustand);
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

            System.out.println("--------------------------");

            pfadfinder_enheth.starteUeberquerung(qLearningSee, false,false,false);
            qLearningSee.anzeigen();

            anzahlSchritte = 0;
            naechsterZustand = Zustand.Start;
            do {
                Richtung r = pfadfinder_enheth.naechsterSchritt(naechsterZustand);
                System.out.println("Gehe " + r);
                naechsterZustand = qLearningSee.geheNach(r);
                anzahlSchritte++;
                qLearningSee.anzeigen();
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
