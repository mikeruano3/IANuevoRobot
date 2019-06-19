/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nuevorobot;

/**
 *
 * @author miguel
 */
import java.awt.Color;
import robocode.*;

import be.ac.ulg.montefiore.run.jadti.io.ItemSetReader;
import be.ac.ulg.montefiore.run.jadti.DecisionTree.*;
import be.ac.ulg.montefiore.run.jadti.*;
import java.io.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Robo2015 extends AdvancedRobot {

    public static final double ROBOT_SIZE = 36;
    private final static String INPUT_PREFIX = "argumentos\n"
            + "escaneado symbolic distancia numerical energiaEnemigo numerical apuntandoAEnemigo symbolic "
            + "golpeoBala symbolic golpeoPared symbolic golpeoEnemigo symbolic energia numerical "
            + "armaCaliente symbolic X numerical Y numerical enMovimiento symbolic armaGirando symbolic "
            + "robotGirando symbolic accion symbolic\n";

    private ScannedRobotEvent scannedRobot;
    private HitByBulletEvent hitByBulletEvent;
    private HitWallEvent hitWallEvent;
    private HitRobotEvent hitRobotEvent;
    private double maxDistance;

    public void run() {
        setColors(Color.black, Color.darkGray, Color.red); // body,gun,radar

        /**
         * CONFIGURACION DEL ARBOL DE DECISION *
         */
        ItemSet learningSet = null;
        String datosAprendizaje = "argumentos\n"
                + "escaneado symbolic distancia numerical energiaEnemigo numerical apuntandoAEnemigo symbolic "
                + "golpeoBala symbolic golpeoPared symbolic golpeoEnemigo symbolic energia numerical "
                + "armaCaliente symbolic X numerical Y numerical enMovimiento symbolic armaGirando symbolic "
                + "robotGirando symbolic accion symbolic\n"
                + "true 1 100 true false false false 100 false ? ? ? ? ? disparoLeve\n"
                + "true 0.5 100 true false false false 100 false ? ? ? ? ? disparoMedio\n"
                + "true 0.1 100 true false false false 100 false ? ? ? ? ? disparoFuerte\n"
                + "true 1 50 true false false false 100 false ? ? ? ? ? disparoMedio\n"
                + "true ? 100 true false false false 10 false ? ? ? ? ? disparoLeve\n"
                + "true 0.8 20 true false false false 80 false ? ? ? ? ? disparoMedio\n"
                + "true ? ? false false false false ? ? ? ? true ? ? apuntar\n"
                + "false ? ? false false ? ? ? ? ? ? true false ? moverArma\n"
                + "false ? ? ? false ? ? ? ? ? ? true true ? nada\n"
                + "? ? ? ? ? ? ? ? ? 0.25 0.75 false ? false moverSE\n"
                + "? ? ? ? ? ? ? ? ? 0.25 0.25 false ? false moverNE\n"
                + "? ? ? ? ? ? ? ? ? 0.75 0.25 false ? false moverNW\n"
                + "? ? ? ? ? ? ? ? ? 0.75 0.75 false ? false moverSW\n"
                + "? ? ? ? ? true ? ? ? ? ? ? ? false mediaVuelta\n";

        try {
            learningSet = ItemSetReader.read(new StringReader(datosAprendizaje));
        } catch (IOException ex) {
            Logger.getLogger(Robo2015.class.getName()).log(Level.SEVERE, null, ex);
        } catch (FileFormatException ex) {
            Logger.getLogger(Robo2015.class.getName()).log(Level.SEVERE, null, ex);
        }

        AttributeSet attributeSet = learningSet.attributeSet();

        Vector testAttributesVector = new Vector();
        testAttributesVector.add(attributeSet.findByName(Memoria.escaneado));
        testAttributesVector.add(attributeSet.findByName(Memoria.distancia));
        testAttributesVector.add(attributeSet.findByName(Memoria.energiaEnemigo));
        testAttributesVector.add(attributeSet.findByName(Memoria.apuntandoAEnemigo));
        testAttributesVector.add(attributeSet.findByName(Memoria.golpeoBala));
        testAttributesVector.add(attributeSet.findByName(Memoria.golpeoPared));
        testAttributesVector.add(attributeSet.findByName(Memoria.golpeoEnemigo));
        testAttributesVector.add(attributeSet.findByName(Memoria.energia));
        testAttributesVector.add(attributeSet.findByName(Memoria.armaCaliente));
        testAttributesVector.add(attributeSet.findByName(Memoria.X));
        testAttributesVector.add(attributeSet.findByName(Memoria.Y));
        testAttributesVector.add(attributeSet.findByName(Memoria.enMovimiento));
        testAttributesVector.add(attributeSet.findByName(Memoria.armaGirando));
        testAttributesVector.add(attributeSet.findByName(Memoria.robotGirando));

        AttributeSet testAttributes = new AttributeSet(testAttributesVector);
        SymbolicAttribute goalAttribute = (SymbolicAttribute) attributeSet.findByName(Memoria.accion);

        DecisionTreeBuilder builder = new DecisionTreeBuilder(learningSet, testAttributes, goalAttribute);
        DecisionTree tree = builder.build().decisionTree();

        maxDistance = Math.sqrt(getBattleFieldHeight() * getBattleFieldHeight() + getBattleFieldWidth() * getBattleFieldWidth());

        // Robot main loop
        while (true) {
            
            String accion = "";
            try {
                String estadoActual = obtenerEstadoActual();                
                KnownSymbolicValue guessedGoalAttributeValue = tree.guessGoalAttribute(ItemSetReader.read(new StringReader(estadoActual), attributeSet).item(0));
                accion = tree.getGoalAttribute().valueToString(guessedGoalAttributeValue);
            } catch (IOException ex) {
                Logger.getLogger(Robo2015.class.getName()).log(Level.SEVERE, null, ex);
            } catch (FileFormatException ex) {
                Logger.getLogger(Robo2015.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            switch(accion)
            {
                case Memoria.DISPAROFUERTE:
                    setFire(4);
                    break;
                    
                case Memoria.DISPAROMEDIO:
                    setFire(2);
                    break;
                    
                case Memoria.DISPAROLEVE:
                    setFire(1);
                    break;
                    
                case Memoria.MOVERARMA:
                    setTurnGunRight(360);
                    break;
                    
                case Memoria.APUNTAR:
                    setTurnGunRight((scannedRobot.getBearing() + getHeading() + 360)%360 - getGunHeading());
                    break;
                    
                case Memoria.MEDIAVUELTA:
                    setTurnRight(180);
                    break;
                
                case Memoria.MOVER_SE:
                    setTurnRight(135 - getHeading());
                    setAhead(Math.sqrt(Math.pow(getBattleFieldWidth()*0.7d - getX(), 2) + Math.pow(getBattleFieldHeight()*0.3 - getY(), 2)));
                    break;
                
                case Memoria.MOVER_NE:
                    setTurnRight(45 - getHeading());
                    setAhead(Math.sqrt(Math.pow(getBattleFieldWidth()*0.7d - getX(), 2) + Math.pow(getBattleFieldHeight()*0.7 - getY(), 2)));
                    break;
                
                case Memoria.MOVER_NW:
                    setTurnRight(315 - getHeading());
                    setAhead(Math.sqrt(Math.pow(getBattleFieldWidth()*0.3d - getX(), 2) + Math.pow(getBattleFieldHeight()*0.7 - getY(), 2)));
                    break;
                
                case Memoria.MOVER_SW:
                    setTurnRight(225 - getHeading());
                    setAhead(Math.sqrt(Math.pow(getBattleFieldWidth()*0.3d - getX(), 2) + Math.pow(getBattleFieldHeight()*0.3 - getY(), 2)));
                    break;
            }
            
            scannedRobot = null;
            hitByBulletEvent = null;
            hitWallEvent = null;
            hitRobotEvent = null;
        }
    }

    private String obtenerEstadoActual() {
        StringBuilder builder = new StringBuilder(INPUT_PREFIX);

        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("##0.00", decimalFormatSymbols);

        scan();
        if (scannedRobot != null) {
            //escaneado
            builder.append("true ");
            //distancia
            builder.append(decimalFormat.format(scannedRobot.getDistance() / maxDistance)).append(' ');
            //energiaEnemigo
            builder.append(decimalFormat.format(scannedRobot.getEnergy())).append(' ');
            //apuntandoAEnemigo
            builder.append(Math.abs((scannedRobot.getBearing() + getHeading() + 360) % 360 - getGunHeading()) <= Math.abs(2 * 180 * Math.atan(ROBOT_SIZE * 0.5d / scannedRobot.getDistance() / Math.PI)) ? "true " : "false ");
        } else {
            //escaneado
            builder.append("false ");
            //distancia
            builder.append("? ");
            //energiaEnemigo
            builder.append("? ");
            //apuntandoAEnemigo
            builder.append("false ");
        }
        
        //golpeoBala
        builder.append(hitByBulletEvent != null ? "true " : "false ");
        //golpeoPared
        builder.append(hitWallEvent != null ? "true " : "false ");
        //golpeoEnemigo
        builder.append(hitRobotEvent != null ? "true " : "false ");
        //energia
        builder.append(decimalFormat.format(getEnergy())).append(' ');
        //armaCaliente
        builder.append(getGunHeat() > 0 ? "true " : "false ");
        //X
        builder.append(decimalFormat.format(getX() / getBattleFieldWidth())).append(' ');
        //Y
        builder.append(decimalFormat.format(getY() / getBattleFieldHeight())).append(' ');
        //enMovimiento
        builder.append(getDistanceRemaining() != 0 ? "true " : "false ");
        //armaGirando
        builder.append(getGunTurnRemaining() != 0 ? "true " : "false ");
        //robotGirando
        builder.append(getTurnRemaining() != 0 ? "true " : "false ");
        //accion
        return builder.append('?').append('\n').toString();
    }

    /**
     * onScannedRobot: What to do when you see another robot
     * @param e
     */
    @Override
    public void onScannedRobot(ScannedRobotEvent e) {
        // Replace the next line with any behavior you would like
        scannedRobot = e;
    }

    /**
     * onHitByBullet: What to do when you're hit by a bullet
     * @param e
     */
    @Override
    public void onHitByBullet(HitByBulletEvent e) {
        // Replace the next line with any behavior you would like
        hitByBulletEvent = e;
//        setTurnRight(10000);
//        ahead(100);
//        setTurnLeft(10000);
//        ahead(100);
    }

    /**
     * onHitWall: What to do when you hit a wall
     * @param e
     */
    @Override
    public void onHitWall(HitWallEvent e) {
        // Replace the next line with any behavior you would like
        hitWallEvent = e;
    }

    /**
     * onHitRobot: What to do when you hit a robot
     * @param e
     */
    @Override
    public void onHitRobot(HitRobotEvent e) {
        // Replace the next line with any behavior you would like
        hitRobotEvent = e;
    }
}

