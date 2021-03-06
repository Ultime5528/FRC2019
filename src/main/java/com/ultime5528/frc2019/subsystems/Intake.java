package com.ultime5528.frc2019.subsystems;

import com.ultime5528.frc2019.K;
import com.ultime5528.frc2019.Robot;

import badlog.lib.BadLog;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.command.Subsystem;

public class Intake extends Subsystem {

    private VictorSP moteurRouleauHaut;
    private VictorSP moteurRouleauBas;
    private DigitalInput photocell;

    public Intake() {

        moteurRouleauHaut = new VictorSP(K.Ports.INTAKE_MOTEUR_HAUT);
        addChild("Moteur du rouleau haut", moteurRouleauHaut);
        BadLog.createTopic("Intake/Puissance moteur rouleau haut", "%", () -> moteurRouleauHaut.get());

        moteurRouleauBas = new VictorSP(K.Ports.INTAKE_MOTEUR_BAS);
        addChild("Moteur du rouleau bas", moteurRouleauBas);
        BadLog.createTopic("Intake/Puissance moteur rouleau bas", "%", () -> moteurRouleauBas.get());

        photocell = new DigitalInput(K.Ports.INTAKE_PHOTOCELL);
        addChild("Photocell", photocell);
        BadLog.createTopic("Intake/Photocell valeur", BadLog.UNITLESS, () -> {
            if (photocell.get() == true) {
                return 1.0;
            } else {
                return -1.0;
            }
        });

    }

    @Override
    protected void initDefaultCommand() {

    }

    public void prendreBallon() {
        moteurRouleauHaut.set(K.Intake.MOTEUR_HAUT_PRENDRE_BALLON);
        moteurRouleauBas.set(K.Intake.MOTEUR_BAS_PRENDRE_BALLON);

    }
    public void grimper(){ 
        moteurRouleauBas.set(Math.min(-0.3, -3*Robot.oi.getJoystick().getY()));

    } 

    public void lancer(){
        moteurRouleauHaut.set(-K.Intake.MOTEUR_HAUT_PRENDRE_BALLON);
        moteurRouleauBas.set(-K.Intake.MOTEUR_BAS_PRENDRE_BALLON);

    }


    public void transfererBallon() {
        moteurRouleauHaut.set(K.Intake.MOTEUR_HAUT_TRANSFERER_BALLON);
        moteurRouleauBas.set(K.Intake.MOTEUR_BAS_TRANSFERER_BALLON);

    }

    public void arreterMoteurs() {
        moteurRouleauHaut.set(0.0);
        moteurRouleauBas.set(0.0);

    }

    public boolean ballonPresent() {
        return photocell.get();
    }

}
