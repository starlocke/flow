// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which provides various comb-style filters to a sound, with the option of modulating
   the size and position of the comb lobes much like flanging and chorusing do.  The shape of
   these lobes is far from exact with regard to combs; they are a very rough approximation meant
   to be fast.  There are five options, some of which are way out there.  The classic comb filter
   is SPIKE DOWN, with the negative comb filter being SPIKE UP.  You have the option of including
   the fundamental in the filter (or not).  The base filter frequency can be FIXED or not.  If not,
   then the frequency shifts according to the current note pitch.
   
   <p>At present there are five modulation options: this may change.  The AMOUNT is the degree of
   wetness of the filter.  The OFFSET and OFFSET MOD both shift the lobe position; OFFSET MOD is intended
   to be attached to a modulation source like an LFO, with OFFSET providing the base offset.  Similarly,
   STRETCH and STRETCH MOD change the lobe size; once again, STRETCH MOD is meant to be attached to an
   LFO or the like, while STRETCH provides the base size.
*/

public class FlangeFilter extends Unit
    {       
    private static final long serialVersionUID = 1;

    int style = STYLE_SPIKE_DOWN;

    public static final int STYLE_LINEAR = 0;
    public static final int STYLE_SPIKE_UP = 1;
    public static final int STYLE_SPIKE_DOWN = 2;
    public static final int STYLE_CURVY = 3;
    public static final int STYLE_SPIKEY = 4;
        
    public static final String[] STYLE_NAMES = new String[] { "Linear", "Spike Up", "Spike Down", "Curvy", "Spikey" };

    public int getStyle() { return style; }
    public void setStyle(int val) { style = val; }

    boolean fixed = false;
    public boolean getFixed() { return fixed; }
    public void setFixed(boolean val) { fixed = val; }
        
    boolean includeFundamental = true;
    public boolean getIncludeFundamental() { return includeFundamental; }
    public void setIncludeFundamental(boolean val) { includeFundamental = val; }
        
    public static final int OPTION_STYLE = 0;
    public static final int OPTION_FIXED = 1;
    public static final int OPTION_INCLUDE_FUNDAMENTAL = 2;
        
    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_STYLE: return getStyle();
            case OPTION_FIXED: return getFixed() ? 1 : 0;
            case OPTION_INCLUDE_FUNDAMENTAL: return getIncludeFundamental() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_STYLE: setStyle(value); return;
            case OPTION_FIXED: setFixed(value != 0); return;
            case OPTION_INCLUDE_FUNDAMENTAL: setIncludeFundamental(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
    
    public static final int MOD_AMOUNT = 0;
    public static final int MOD_OFFSET = 1;
    public static final int MOD_OFFSET_MOD = 2;
    public static final int MOD_STRETCH = 3;
    public static final int MOD_STRETCH_MOD = 4;
    
    public FlangeFilter(Sound sound) 
        { 
        super(sound);   
        defineInputs( new Unit[] { Unit.NIL }, new String[] { "Input" });
        defineModulations(new Constant[] {  Constant.ONE, Constant.ZERO, Constant.HALF, Constant.HALF, Constant.ZERO },
            new String[] { "Wet", "Offset", "OffsetMod", "Stretch", "StretchMod"});
        defineOptions(new String[] { "Style", "Fixed", "Fundamental" }, new String[][] { STYLE_NAMES, { "Fixed" }, { "Fundamental" } } );
        }
        
    public void go()
        {
        super.go();
                
        pushFrequencies(0);

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double[] inAmp = getAmplitudesIn(0);
                
        double pitch = getSound().getPitch();
                
        // The filter stretch (lobe size) is presently the BASE LOBE SIZE + lobe scale * modulation
                
        double filterStretch = makeSensitive(modulate(MOD_STRETCH)) * 22049 + 1 + 22049 * modulate(MOD_STRETCH_MOD);

        // The filter frequency is presently the BASE frequency + lobe size * number of lobes * modulation
                
        double filterFreq = modToFrequency(modulate(MOD_OFFSET)) + ( fixed ? 0 : pitch) + filterStretch * (modulate(MOD_OFFSET_MOD) - 0.5) * 4;
                                
        double gainMod = modulate(MOD_AMOUNT);
                                
        for(int i = 0; i < amplitudes.length; i++)
            {
            if (i == 0 && !includeFundamental) { amplitudes[i] =  inAmp[i]; continue; }
                        
            // we need to figure out where in the lobe (or really "trough") we are.  We define our POSITION in
            // the lobe as a value 0..1 where 0 is located at the full height of the previous lobe and 1 is
            // at the full height of the next lobe.  That way the fundamental is at full height by default.
            //
            // The baseline frequency is filterFreq.                f
            // The width of a lobe is filterStretch.                s
            // Our input frequency is a
            //
            // position = ((((a - f) % s) + s) % s) / s

            double a = frequencies[i] * pitch;
            double f = filterFreq;
            double s = filterStretch;
            double pos = (((a - f) % s + s) % s) / s;
                        
            // Now we're between 0 and 1.  Consider reflections
            double p = (pos < 0.5 ? pos : 1.0 - pos) * 2;
            // still between 0 and 1

            double gain = 0;
            switch (style)
                {
                case STYLE_LINEAR:
                    {
                    gain = (1.0 - p);
                    }
                break;
                case STYLE_SPIKE_UP:
                    {
                    gain = (1 - p) * (1 - p) * (1 - p);
                    }
                break;
                case STYLE_SPIKE_DOWN:
                    {
                    gain = 1 - p * p * p;
                    }
                break;
                case STYLE_CURVY:
                    {
                    if (p < 0.5)
                        gain = 1 - 4 * p * p * p;
                    else
                        gain = -4 * (p - 1) * (p - 1) * (p - 1);
                    }
                break;
                case STYLE_SPIKEY:
                    {
                    if (p < 0.5)
                        gain = (1 + (1 - p * 2) * (1 - p * 2) * (1 - p * 2)) / 2;
                    else
                        gain = (1 - (2 * p - 1) * (2 * p - 1) * (2 * p - 1)) / 2;
                    }
                }

            // Gain is right now 0...1.  We need to make it 0...2 centered at 1 with a variable modulation
            gain -= 0.5;
            gain *= 2.0;
                        
            // Now we go -1...1
            gain *= gainMod;
            gain += 1.0;
                        
            // Finally we multiply it in
            amplitudes[i] = inAmp[i] * gain;
            }

        constrain();
        }       
    }