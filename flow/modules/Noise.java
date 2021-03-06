// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;
import java.util.*;

/**
   A Unit which creates random noise and hiss by creating and immediately
   destroying entirely random partials.  PARTIALS specifies how many partials
   are employed to produce the hiss.  All partials not employed are set to
   amplitude = 0.  If TOP is true, then the partials not employed will be the
   LOWER partials, and will all have frequency = 0;.  If TOP is false, then
   the partials not employed will be the HIGHER partials, and will all have
   a frequency somewhat (x 1.1) higher than the high bound frequency.
   
   <p>The modulations HIGH and LOW define the high and low bounds
   of the frequency range for the partials.  This frequency range
   is fixed and independent of pitch.  AMP VAR specifies the degree of
   variance in the amplitudes of the random partials.  RAMP specifies
   the degree to which the random partials ramp up from 0 amplitude
   near the lowest partial to a maximum amplitude near the highest partial,
   as opposed to having all the partials be the same amplitude (subject to
   AMP VAR).  GAIN specifies the average volume of the partials.
   
   <p>This module, when set to TOP=TRUE is particularly useful in combination with the Fill
   module, where any partials not allocated to Noise are then used
   for some other incoming Unit like Sawtooth or whatnot.  You might
   want to set TOP=FALSE when doing other kinds of merges which otherwise steal
   from the topmost partials.  You'll find that
   you can do really good noise with just a few partials, leaving the
   remainder to the other Unit in question.
*/

public class Noise extends Unit implements UnitSource
    {
    private static final long serialVersionUID = 1;

    public static final int MOD_HIGH = 0;
    public static final int MOD_LOW = 1;
    public static final int MOD_PARTIALS = 2;
    public static final int MOD_AMP_VAR = 3;
    public static final int MOD_RAMP = 4;
    public static final int MOD_GAIN = 5;
    public static final int MOD_SEED = 6;

    public Random random = null;
        
    void initializeRandom()     
        {
        // first reseed
        double mod = modulate(MOD_SEED);
                
        if (mod == 0)
            {
            random = null;
            }
        else
            {
            long seed = Double.doubleToLongBits(mod);
            if (random == null) random = new Random(seed);
            else random.setSeed(seed);
            }
        }
        
    public void reset()
        {
        super.reset();
        initializeRandom(); 
        }
                
    public void gate()
        {
        super.gate();
        initializeRandom(); 
        }

    boolean top = true;
    public boolean isTop() { return top; }
    public void setTop(boolean val) { top = val; }
        
    public static final int OPTION_TOP = 0;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_TOP: return isTop() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_TOP: setTop(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }


    public Noise(Sound sound)
        {
        super(sound);
        defineModulations(new Constant[] { Constant.ONE, Constant.ZERO, Constant.HALF, Constant.ZERO, Constant.ONE, Constant.HALF, Constant.ZERO }, 
            new String[] { "High", "Low", "Partials", "Amp Var", "Ramp", "Gain", "Seed" });
        defineOptions(new String[] { "Top" }, new String[][] { { "Top" } } );
        }
    
    public void generateRandomVals(double[] freq, int start, int n, Random random)
        {
        /*
          for(int i = start; i < start + n; i++)
          {
          freq[i] = random.nextDouble();
          }
          Arrays.sort(freq, start, start + n);
        */
        
        // from Bentley, "Generating Sorted Lists of Random Numbers"
        // You'd presume this is faster than an O(n lg n ) sort, but it's basically
        // identical in speed due to the calls to log.
        double sum = 0;
        for(int i = start; i < start + n; i++)
            {
            double d = random.nextDouble();
            sum = sum - Math.log(d);
            freq[i] = sum;
            }
        double d = random.nextDouble();
        sum = sum - Math.log(d);
        double invsum = 1.0 / sum;
        for(int i = start; i < start + n; i++)
            freq[i] = freq[i] * invsum;
        }

    public void go()
        {
        int partials = (int)(modulate(MOD_PARTIALS) * Unit.NUM_PARTIALS);
        
        double _var = modulate(MOD_AMP_VAR);
        double ramp = modulate(MOD_RAMP);
        double gain = modulate(MOD_GAIN);
        double lo = modToInsensitiveFrequency(modulate(MOD_LOW));
        double hi = modToInsensitiveFrequency(modulate(MOD_HIGH));
        if (hi < lo) { double temp = hi; hi = lo; lo = temp; }
        
        double total = 0;
        double pitch = sound.getPitch();
        double invpitch = 1.0 / pitch;

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = 0.0;
            frequencies[i] = (top ? 0 : hi * 1.1 * invpitch);
            }
        
        if (partials == 0) return;

        Random rand = (random == null ? getSound().getRandom() : random);
        
        int start = (top ? Unit.NUM_PARTIALS - partials : 0);
        int end = (top ? Unit.NUM_PARTIALS : partials);
        
        generateRandomVals(frequencies, start, partials, rand);
        
        double max = 0;
        for(int j = start; j < end; j++)
            {
            double pos = frequencies[j];
            frequencies[j] = (pos * (hi - lo) + lo) * invpitch;
            double ramping = 0;
            if (ramp >= 0.5)
                {
                ramping = (1 - (ramp - 0.5) * 2) * 1 + (ramp - 0.5) * 2 * pos;
                }
            else
                {
                ramping = ramp * 2 * 1 + (1 - ramp * 2) * (1 - pos);
                }
            if (_var == 0.0)
                amplitudes[j] = ramping;  
            else
            	{
            	double d = rand.nextDouble();
                amplitudes[j] = (1-_var) * ramping + _var * d;
                }
            total += amplitudes[j];
            if (amplitudes[j] > max) max = amplitudes[j];
            }
        
        // normalize
        for(int j = start; j < end; j++)
            {
            amplitudes[j] = amplitudes[j] * gain;
            }
        }
    }
