/*
For each note
you need
freq
samples
envelope state trackers - noteOn, noteOff, and state

the position is universal, but you mod it by the period for each note
 */

package org.craigl.softsynth;

import java.util.ArrayList;
import org.apache.commons.math3.fraction.*;

/**
 * BasicOscillator Class
 * <p>
 * A non bandwidth controlled digital oscillator which can produce three waveshapes.
 * <p>
 * See text for details.
 * 
 * @author craiglindley
 */

public class BasicOscillator implements SampleProviderIntfc {
	
	public static final int EQUAL_TEMPERED = 0;
	public static final int JUST_SEVEN_LIMIT = 1;
	public static final int JUST_FIVE_EXT = 2;
	public static final int JUST_FIVE_SYM = 3;
	public static final int PYTHAGOREAN = 4;

	public final static int NUM_NOTES = 23;
	
	VCA vca;
	
	//Each array is indexed by the note number, where 0 is A2 and 22 is G4
	
	//The position of the sample in the waveform for each note
	private double sampleNumber[] = new double[NUM_NOTES];
	
	//The note frequencies
	public double[] equalTempFreq = new double[NUM_NOTES];
	public double[] noteFreq = new double[NUM_NOTES];
	
	private String[] tuningStrings = { "1/1, 16/15, 8/7, 6/5, 5/4, 4/3, 7/5, 3/2, 8/5, 5/3, 7/4, 15/8",
									  "1/1, 16/15, 9/8, 6/5, 5/4, 4/3, 25/18, 3/2, 8/5, 5/3, 9/5, 15/8",
									  "1/1, 16/15, 9/8, 6/5, 5/4, 4/3, 45/32, 3/2, 8/5, 5/3, 16/9, 15/8",
									  "1/1, 256/243, 9/8, 32/27, 81/64, 4/3, 729/512, 3/2, 128/81, 27/16, 16/9, 243/128"};
	
	public Fraction[][] tuneRatios;
	
	public ArrayList<double[]> tunings;	

	//The number of samples in one period of the note
	//The note being a sine wave of the given frequency
	public double[] notePeriod = new double[NUM_NOTES];
	
	//These are the magnitudes of the harmonics, used in additive synthesis
	public double[] harmoAmp = {.8, .6, .4, .4};
	//Sum of the elements of harmoAmp. Used to scale down samples, so adding harmonics doesn't make everything louder.
	public double harmoSum;
	
	/**
	 * Basic Oscillator Class Constructor
	 * <p>
	 * Default instance has SIN waveshape at 1000 Hz
	 */
	public BasicOscillator() {

		// Set defaults
		
		//Fill out Equal Tempered, and copy that to the active frequencies
		double temp = 110.0;
		for(int i=0;i<NUM_NOTES;i++){
			equalTempFreq[i] = temp;
			noteFreq[i] = temp;
			temp*=Math.pow(2.0, 1.0/12.0);
		}
		
		for(int i=0;i<notePeriod.length;i++)
			notePeriod[i] = SamplePlayer.SAMPLE_RATE / noteFreq[i];
		
		harmoSum = 0;
		for(int i=0;i<harmoAmp.length;i++){
			harmoSum+=harmoAmp[i];
		}
		
		//Populate the tuning ratio array and the tunings array
		tuneRatios  = new Fraction[tuningStrings.length][12];//12 unique ratios per scale
		
		String[] tempSplit;

		for(int dex=0; dex< tuningStrings.length; dex++){
			String[] tempStrArr = tuningStrings[dex].split(",");
			for(int i=0;i<tempStrArr.length;i++){
				tempSplit = tempStrArr[i].split("/");
				tuneRatios[dex][i] = new Fraction( Integer.parseInt(tempSplit[0].trim()), Integer.parseInt(tempSplit[1].trim()) );
			}
		}
		
		tunings = new ArrayList<double[]>();
		tunings.add(equalTempFreq);
		for(int i=0;i<tuningStrings.length;i++){//TuningStrings.length is the number of alternate tunings
			double[] tempRR = new double[12];
			for(int j=0;j<tempRR.length;j++){
				//System.out.println(i+","+j);
				tempRR[j] = tuneRatios[i][j].doubleValue();
			}
			tunings.add(tempRR);
		}
	}
	
	public void setVCA(VCA vca_in){
		vca = vca_in;
	}

	//Recalculates the noteFreq array based on a chosen tuning.
	//The basenote will always be in the lower octave.
	public void setTuning(int tuneDex, int baseNote){
		switch (tuneDex){
		case EQUAL_TEMPERED://0
			for(int i=0;i<noteFreq.length;i++)
				noteFreq[i] = equalTempFreq[i];
			break;
		default:
			double[] tempTuneArr = tunings.get(tuneDex);
			double temp = noteFreq[baseNote];
			
			//Zero out everything except the chosen base note
			for(int i=0;i<noteFreq.length;i++)
				noteFreq[i]=0.0;
			noteFreq[baseNote] = temp;
			
			//Fill in one chromatic scale, starting from the base note
			for(int i=baseNote; i<baseNote+tempTuneArr.length; i++){
				noteFreq[i] = noteFreq[baseNote] * tempTuneArr[i-baseNote];
			}
			
			//Fill in the rest of the notes as octaves up or down
			for(int i=0;i<noteFreq.length/2;i++){
				if(noteFreq[i]==0)
					noteFreq[i]=noteFreq[i+12]/2;
			}
			for(int i=noteFreq.length/2;i<noteFreq.length;i++){
				if(noteFreq[i]==0)
					noteFreq[i]=noteFreq[i-12]*2;
			}
			break;
		}
	}
	
	/* Return the next sample of the oscillator's waveform
	 * @return Next oscillator sample
	 */
	protected double getSample(int noteDex) {
		
		double value = 0;
		double constMultiplicand = 2.0 * Math.PI * noteFreq[noteDex] * sampleNumber[noteDex] / (double)SamplePlayer.SAMPLE_RATE;
		
		for(int i=0; i<harmoAmp.length;i++){
			value+=harmoAmp[i]*Math.sin((double)(2.0*i+1) * constMultiplicand);
		}
		
		sampleNumber[noteDex]++;
		if(sampleNumber[noteDex] > notePeriod[noteDex])
			sampleNumber[noteDex] -= notePeriod[noteDex];

		return value/harmoSum;
	}
		
	/* Get a buffer of oscillator samples
	 * @param buffer Array to fill with samples
	 * @return Count of bytes produced.
	 */
	//inputNoteDex is -1, useless
	public int getSamples(byte [][] buffer, int inputNoteDex) {
		//setFrequency(inputNoteDex);
		double ds = 0;
		short ss = 0;
		
		//For each note
		for(int i = 0; i<NUM_NOTES; i++){
			if(!vca.noteIsIdle(i)){//if the note is playing
				for(int j = 0; j<SamplePlayer.SAMPLES_PER_BUFFER*2; j+=2){//fill the buffer with samples
					ds = getSample(i) * Short.MAX_VALUE / 3;
					ss = (short) Math.round(ds);
					
					buffer[i][j] = (byte)(ss >> 8);
					buffer[i][j+1] = (byte)(ss & 0xFF);
				}
			}
		}
		
		return SamplePlayer.BUFFER_SIZE;
	}
	
	public double getA(){
		return noteFreq[0]*4;
	}
	
	//-1 since tuneDex of 0 is Equal Tempered, and that doesn't have fractions to give
	public Fraction[] getIntervals(int tuneDex){
		return tuneRatios[tuneDex-1];
	}
	
	public double[] getFrequencies(){
		return noteFreq;
	}

}
