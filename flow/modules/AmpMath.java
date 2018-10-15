// Copyright 2018 by George Mason University
// Licensed under the Apache 2.0 License


package flow.modules;

import flow.*;

/**
   A Unit which takes two incoming Units and performs a variety of math functions
   on the amplitudes of their respective partials.
   
   <p>These math functions include (for incoming partials sources A and B, 
   and an additional modulation source MOD):
   
   <ul>
   <li> ADD: min(1, A + B)
   <li> SUBTRACT: max(0, A - B)
   <li> MULTIPLY: A * B
   <li> INV_MULTIPLY: A * (1 - B)
   <li> COMPRESS: 1 - (1 - A) * (1 - B)
   <li> MEAN: (A + B) / 2
   <li> MIN: min(A, B)
   <li> MAX: max(A, B)
   <li> FILTER: If (B < mod), 0, else A
   <li> FILTERNOT: If (B >= mod), 0, else A
   <li> FILL: If A > mod, A, else B
   <li> THRESHOLD: If A > mod, 1, else 0
   <li> SCALEDOWN: A * mod
   <li> SCALEUP: min(1, A * (mod + 1))
   </ul>
   
   <p>You also have have the option of NORMALIZING the result after the fact.
*/


public class AmpMath extends Unit
    {
    private static final long serialVersionUID = 1;
    public static String getName() { return "Amp Math"; }

    public static final int ADD = 0;
    public static final int SUBTRACT = 1;
    public static final int MULTIPLY = 2;
    public static final int INV_MULTIPLY = 3;
    public static final int COMPRESS = 4;
    public static final int MEAN = 5;
    public static final int MIN = 6;
    public static final int MAX = 7;
    public static final int FILTER = 8;
    public static final int FILTERNOT = 9;
    public static final int FILL = 10;
    public static final int THRESHOLD = 11;
    public static final int SCALEDOWN = 12;
    public static final int SCALEUP = 13;
    public static final int EXPAND = 14;
        
    public static final String[] OPERATION_NAMES = new String[] { 
        "+", "-", "*", "inv *", "compress", "average", "min", "max", "filter", "filternot", 
        "fill", "threshold", "scaledown", "scaleup", }; 
        
    int operation = ADD;
    boolean normalize;
        
    public int getOperation() { return operation; }
    public void setOperation(int val) { operation = val; }
    public boolean getNormalize() { return normalize; };
    public void setNormalize(boolean val) { normalize = val; }
        


    public static final int OPTION_OPERATION = 0;
    public static final int OPTION_NORMALIZE = 1;

    public int getOptionValue(int option) 
        { 
        switch(option)
            {
            case OPTION_OPERATION: return getOperation();
            case OPTION_NORMALIZE: return getNormalize() ? 1 : 0;
            default: throw new RuntimeException("No such option " + option);
            }
        }
                
    public void setOptionValue(int option, int value)
        { 
        switch(option)
            {
            case OPTION_OPERATION: setOperation(value); return;
            case OPTION_NORMALIZE: setNormalize(value != 0); return;
            default: throw new RuntimeException("No such option " + option);
            }
        }
        

    public AmpMath(Sound sound)
        {
        super(sound);
        defineInputs( new Unit[] { Unit.NIL, Unit.NIL }, new String[] { "Input A", "Input B" });
        defineModulations(new Constant[] { Constant.ONE }, new String[] { "Scale" });
        defineOptions(new String[] { "Operation", "Normalize" }, new String[][] { OPERATION_NAMES, { "Normalize"} } );
        setOperation(ADD);
        setNormalize(false);
        }
        
    public void go()
        {
        super.go();

        double[] amplitudes = getAmplitudes(0);
        double[] frequencies = getFrequencies(0);
                
        double[] inputs1amplitudes = getAmplitudesIn(1);
        double[] inputs0amplitudes = getAmplitudesIn(0);
                
        copyFrequencies(0);
        double modulation = modulate(0);

        switch(operation)
            {
            case ADD:           // A + B
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] + inputs1amplitudes[i];
                    }
                }
            break;
            case SUBTRACT:      // A - B
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] - inputs1amplitudes[i];
                    if (amplitudes[i] < 0) amplitudes[i] = 0;
                    }
                }
            break;
            case MULTIPLY:      // A * B
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] * inputs1amplitudes[i];
                    }
                }
            break;
            case INV_MULTIPLY:  // A * (1 - B)
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] * (1 - inputs1amplitudes[i]);
                    }
                }
            break;
            case COMPRESS:   // 1 - (1 - A) * (1 - B)
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = 1 - (1 - inputs0amplitudes[i]) * (1 - inputs1amplitudes[i]);
                    }
                }
            case MEAN:  // mod * A + (1 - mod) * B
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = modulation * inputs0amplitudes[i] + (1 - modulation) * inputs1amplitudes[i];
                    }
                }
            break;
            case MIN:   // min(A, B)
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = Math.min(inputs0amplitudes[i], inputs1amplitudes[i]);
                    }
                }
            break;
            case MAX:   // max(A, B)
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = Math.max(inputs0amplitudes[i], inputs1amplitudes[i]);
                    }
                }
            break;
            case FILTER: // If B < mod, then 0 else A
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs1amplitudes[i] < modulation ? 0 : inputs0amplitudes[i];
                    }
                }
            break;
            case FILTERNOT:  // If B >= mod, then 0 else A
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs1amplitudes[i] >= modulation ? 0 : inputs0amplitudes[i];
                    }
                }
            break;
            case FILL:          // If A > mod then A else B
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] > modulation ? inputs0amplitudes[i] : inputs1amplitudes[i];
                    }
                }
            break;
            case THRESHOLD:     // If A > mod then 1 else 0
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] >= modulation ? 1 : 0;
                    }
                }
            break;
            case SCALEDOWN:     // A * mod
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] * modulation;
                    }
                }
            break;
            case SCALEUP:       // A * (mod + 1)
                {
                for(int i = 0; i < amplitudes.length; i++)
                    {
                    amplitudes[i] = inputs0amplitudes[i] * (modulation + 1.0);
                    }
                }
            break;
            }
                        
        if (normalize)
            normalizeAmplitudes();
        
        constrain();
        }
    }