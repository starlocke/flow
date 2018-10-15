// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

/**
   A Unit which introduces random partials (tinkling), then gradually fades them away.  It has
   several modulations:
   
   <ul>
   <li>Trigger.  When a trigger arrives here, the Unit will (probably) add new partials.
   <li>Decay.  The rate at which the partials die away.
   <li>Volume.  The initial amplitude of new partials.
   <li>Number.  The number of partials to be introduced each trigger (up to 16).
   <li>Probability.  The probability that partials will be introduced in a given trigger.
   </ul>
   
   <p>
   Tinkle handles constraints specially: if you constrain to a certain set of partials,
   Tinkle will only tinkle in those partials; but the number and probability of tinkles will
   remain the same.
*/

public class Tinkle extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    double[] currentAmplitudes = new double[NUM_PARTIALS];
    public Random random = null;
        
    public Tinkle(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.HALF, Constant.HALF, Constant.ONE, Constant.HALF, Constant.ONE, Constant.ONE }, 
            new String[] { "Trigger", "Decay", "Volume", "Number", "Probability", "Seed"   });
        }
                
    public static final int MAX_NUMBER = 16;

    public void gate()
        {
        super.gate();
                
        for(int i = 0; i < currentAmplitudes.length; i++)
            {
            currentAmplitudes[i] = 0;
            }
        reseed();

        tinkle();
        }
        
    public void reset()
        {
        super.reset();
        
        for(int i = 0; i < currentAmplitudes.length; i++)
            {
            currentAmplitudes[i] = 0;
            }
        reseed();

        tinkle();
        }
        
    public void reseed()
        {
        double mod = modulate(5);
        if (mod != 0)
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }
        else if (random != null)
            {
            random = null;
            }
        }
                        
    public void go()
        {
        super.go();
                
        modulate(0);
        if (isTriggered(0))
            {
            tinkle();
            }
                        
        reduce();
        interpolate();
        }
                
    void tinkle()
        {
        int number = (int)(modulate(3) * MAX_NUMBER);
        double probability = modulate(4);
        Random rand = (random == null ? getSound().getRandom() : random);
        if (probability == 1.0 || rand.nextDouble() < probability)
            {
            int[] constrainedPartials = getConstrainedPartials();
            double amp = modulate(2);
            if (constrainedPartials.length > 0)
                {
                for(int i = 0; i < number; i++) 
                    {
                    int q;
                    int harm = constrainedPartials[q = rand.nextInt(constrainedPartials.length)];
                    currentAmplitudes[harm] = amp;
                    }
                }
            }
        }
        
    void reduce()
        {
        double alpha = modulate(1) * 0.01 + 0.99;
        for(int i = 0; i < currentAmplitudes.length; i++)
            {
            currentAmplitudes[i] = currentAmplitudes[i] * alpha;
            }
        }
                
    double INTERPOLATION_ALPHA = 1.0;
    void interpolate()
        {
        double[] amplitudes = getAmplitudes(0);
                
        for(int i = 0; i < currentAmplitudes.length; i++)
            {
            amplitudes[i] = (1 - INTERPOLATION_ALPHA) * amplitudes[i] + INTERPOLATION_ALPHA * currentAmplitudes[i];
            }
        }

    public String getModulationValueDescription(int modulation, double value, boolean isConstant)
        {
        if (isConstant)
            {
            if (modulation == 3)  // it's a time
                {
                return "" + (int)(value * MAX_NUMBER);
                }
            else return super.getModulationValueDescription(modulation, value, isConstant);
            }
        else return "";
        }
    }
