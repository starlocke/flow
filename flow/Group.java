// Copyright 2019 by George Mason University
// Licensed under the Apache 2.0 License


package flow;
import org.json.*;

/**
   The object which represents a patch or subpatch and stores relevant parameters for it.  
   Each Sound in the synthesizer belongs to exactly one Group.
*/

public class Group
    {
    public static final double DEFAULT_GAIN = 1.0;
    public static final String EMPTY_JSON = "{ \"flow\":" + Flow.VERSION + ", modules: [ ] }";

    int channel = Input.CHANNEL_NONE;
    int minNote = 0;
    int maxNote = 127;
    int numRequestedSounds = 0;
    JSONObject patch = new JSONObject(EMPTY_JSON);
    String patchName;
    double gain = DEFAULT_GAIN;;
        
    /** Returns the group's current channel.  This can be any of
    	Input.CHANNEL_NONE, or 0 ... 15.  If this group is the Primary group,
    	then the channel can also be Input.CHANNEL_OMNI, Input.CHANNEL_LOWER_ZONE,
    	or Input.CHANNEL_UPPER_ZONE */
    public int getChannel() { return channel; }

    /** Sets the group's current channel.  This can be any of
    	Input.CHANNEL_NONE, or 0 ... 15.  If this group is the Primary group,
    	then the channel can also be Input.CHANNEL_OMNI, Input.CHANNEL_LOWER_ZONE,
    	or Input.CHANNEL_UPPER_ZONE */
    public void setChannel(int c) 
        { 
        if (c < Input.CHANNEL_NONE) 
            c = Input.CHANNEL_NONE; 
        else if (c >= Input.NUM_MIDI_CHANNELS)
            c = Input.CHANNEL_NONE;
        channel = c; 
        }

	/** Returns the minimum note in the Group's note range.  Sounds will not respond
		to notes below this value.  This can any value 0 ... 127 */         
    public int getMinNote() { return minNote; }

	/** Sets the minimum note in the Group's note range.  Sounds will note respond
		to notes below this value.  This can any value 0 ... 127.  If the
		passed in value is greater than the max note, then it is set to the max note. */         
    public void setMinNote(int n) 
        { 
        minNote = n; 
        if (minNote > 127) minNote = 127;
        if (minNote < 0) minNote = 0;
        if (minNote > maxNote)
            minNote = maxNote;
        }

	/** Returns the maximum note in the Group's note range.  Sounds will not respond
		to notes above this value.  This can any value 0 ... 127 */         
    public int getMaxNote() { return maxNote; }

	/** Sets the maximum note in the Group's note range.  Sounds will not respond
		to notes below this value.  This can any value 0 ... 127.  If the
		passed in value is less than the min note, then it is set to the min note. */         
    public void setMaxNote(int n) 
        { 
        maxNote = n; 
        if (maxNote > 127) maxNote = 127;
        if (maxNote < 0) maxNote = 0;
        if (minNote > maxNote)
            maxNote = minNote;
        }
    
    /** Returns true if the note is between the min and max notes, inclusive. */
    public boolean isNoteInRange(int note) { return note >= minNote && note <= maxNote; }
    
    /** Returns the number of requested sounds. */
    public int getNumRequestedSounds() { return numRequestedSounds; }

    /** Sets the number of requested sounds. */
    public void setNumRequestedSounds(int n) { if (n < 0) n = 0; numRequestedSounds = n; }
    
    /** Returns the group's patch. */
    public JSONObject getPatch() { return patch; }

    /** Sets the group's patch. */
    public void setPatch(JSONObject p) 
        { 
        if (p == null)
            p = new JSONObject(EMPTY_JSON);
        patch = p; 
        }

    /** Returns the group's patch name. */
    public String getPatchName() { return patchName; }

    /** Sets the group's patch name. */
    public void setPatchName(String p) 
        { 
        if (p == null)
            p = "";
        patchName = p; 
        }

    /** Returns the group's current gain value (normally 0...1). */
    public double getGain() { return gain; }
    
    /** Sets the group's current gain value (normally 0...1). */
    public void setGain(double g) { if (g < 0) g = 0; gain = g; }
    }
        
