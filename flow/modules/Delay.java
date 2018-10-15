// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/** 
    A Unit adds a delay effect to an incoming signal.  The delay consists
    of an INITIAL one-time delay, and then an iterative sequence of LATER
    delays.  Both the one-time delay and the later delays have cut-down amounts
    in the delay.  You can also specify the wetness.  The maximum delay length
    is n/16 second, where n is Output.SKIP.  We may want to change this to be
    a fixed value regardless of the setting of SKIP.
*/


public class Delay extends Unit
    {
    private static final long serialVersionUID = 1;

    public Delay(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] { 
                Constant.ZERO, Constant.ZERO, 
                Constant.ZERO, Constant.ZERO, Constant.ZERO }, 
            new String[] { "Wet", "Initial Delay", "Later Delays", 
                           "Initial Cut", "Later Cuts" });
        }

    public static final int MAX_INITIAL_DELAY_LENGTH = (int)(Output.SAMPLING_RATE / 16);
    public static final int MAX_LATER_DELAY_LENGTH = (int)(Output.SAMPLING_RATE / 16);
    int initialDelayPos = 0;
    int laterDelayPos = 0;
    double[][] initialDelayBuf = new double[MAX_INITIAL_DELAY_LENGTH][NUM_PARTIALS];
    double[][] laterDelayBuf = new double[MAX_LATER_DELAY_LENGTH][NUM_PARTIALS];
        
        
    public void reset()
        {
        for(int i = 0; i < initialDelayBuf.length; i++)
            {
            double[] initialDelayBufP = initialDelayBuf[i];
            for(int j = 0; j < initialDelayBufP.length; j++)
                initialDelayBufP[j] = 0;
            }
        for(int i = 0; i < laterDelayBuf.length; i++)
            {
            double[] laterDelayBufP = laterDelayBuf[i];
            for(int j = 0; j < laterDelayBufP.length; j++)
                laterDelayBufP[j] = 0;
            }
        }
        
    public void go()
        {
        super.go();
                
        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double wet = modulate(0);
        int initialDelay = (int)(makeSensitive(modulate(1)) * MAX_INITIAL_DELAY_LENGTH) + 1;  // so we can't have zero
        if (initialDelay > MAX_INITIAL_DELAY_LENGTH)
            initialDelay = MAX_INITIAL_DELAY_LENGTH;
        int laterDelay = (int)(makeSensitive(modulate(2)) * MAX_LATER_DELAY_LENGTH) + 1;  // so we can't have zero
        if (laterDelay > MAX_LATER_DELAY_LENGTH)
            laterDelay = MAX_LATER_DELAY_LENGTH;
        double initialCut = modulate(3);
        double laterCut = modulate(4);
        copyFrequencies(0);
        double[] amps = getAmplitudesIn(0);
        
        // first load the initialDelay
        int loadpos = initialDelayPos - 1;
        if (loadpos < 0) loadpos += initialDelay;
        System.arraycopy(amps, 0, initialDelayBuf[loadpos], 0, amps.length);

        // next load the laterDelay
        loadpos = laterDelayPos - 1;
        if (loadpos < 0) loadpos += laterDelay;
                
        double[] d = laterDelayBuf[loadpos];
        double[] d2 = initialDelayBuf[initialDelayPos];
        for(int i = 0; i < amps.length; i++)
            {
            d[i] = d2[i] * initialCut + d[i] * laterCut;
            }

        // next roll in the laterDelay
        d = laterDelayBuf[laterDelayPos];
        for(int i = 0; i < amplitudes.length; i++)
            {
            amplitudes[i] = d[i] * wet + amps[i] * (1.0 - wet);
            }
                
        initialDelayPos++;
        if (initialDelayPos >= initialDelay)
            initialDelayPos -= initialDelay;
        if (initialDelayPos >= initialDelay)  // hmmmm
            initialDelayPos = initialDelayPos % initialDelay;
                        
        laterDelayPos++;
        if (laterDelayPos >= laterDelay)
            laterDelayPos -= laterDelay;
        if (laterDelayPos >= laterDelay)
            laterDelayPos = laterDelayPos % laterDelay;

        constrain();
        }

        
    }