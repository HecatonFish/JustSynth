package org.ssg.justsynth;

import org.apache.commons.math3.fraction.Fraction;
import org.craigl.softsynth.*;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.KeyListener;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.state.BasicGameState;
import org.newdawn.slick.state.StateBasedGame;


public class SynthState extends BasicGameState implements KeyListener{

	public int stateID;
	
	BasicOscillator osc;
	VCA vca;
	SamplePlayer player;
	
	//Used to prevent display until the screen is switched
	boolean black;
	
	//Arrays of data to draw the piano keys in order
	//There are 23 unique notes
	//                         A    A+   B    C    C+    D     D+    E    F    F+   G   G+ | Then just add 420
	private int xOffset = -70;
	private int[] xCoords = { 150, 190, 210, 270, 310 , 330,  370  ,390, 450, 490 ,510, 550  ,0,0,0,0,0,0,0,0,0,0,0};
	private int yCoord = 480;
	//White keys are 180px tall, black keys are 100px tall.
	private int[] heights ={ 180, 100, 180, 180, 100 ,  180,  100  ,180, 180, 100, 180, 100,
							 180, 100, 180, 180, 100 ,  180,  100  ,180, 180, 100, 180};
	private int[] widths = new int[BasicOscillator.NUM_NOTES];
	
	//Whether the key is being held down. Used in drawing the keyboard.
	private boolean[] noteOn = new boolean[BasicOscillator.NUM_NOTES];
	private Color keyColor;
	
	//The index is the keyboard key number (explained in comments at the KeyPressed() method)
	//The value is the note to be played upon pressing that key
	//A value of -1 means the key is unused
	//Backslash (Key 43) is handled with a special case
	//For example, keyMap[8] is 11. This means that pressing key 8 (The "7" Key) plays the 11th note in the scale.
	private int[] keyMap = {-1, -1, 1, -1, 4, 6, -1, 9, 11, 13, -1, 16, 18, -1, 21, 0, 2, 3, 5, 7, 8, 10, 12, 14, 15, 17, 19, 20};
	
	//The names of the keys to be drawn to the screen, in ascending note order.
	private String[] keyNames = {">","1","Q","W","3","E","4","R","T","6",
							"Y","7","U","8","I","O","0","P","-","[","]","«","\\"};
	
	//Data for drawing the note ratios table
	private String[][][] table;
	private String[] rowHeaders = {"A","A#","B","C","C#","D","D#","E","F","F#","G","G#"};
	private String[] colHeaders = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11"};
	
	private String[] tuningNames = {"Equal Tempered", "Just 7-Limit", "Just 5-Limit (Extended)", "Just 5-Limit (Symmetric)", "Pythagorean"};
	
	private double[][] centIntervals;
	private int tuning;
	private int baseNote;
	
	//If displayCents is true, show cents, else, show fractions, in the table
	private boolean displayCents;
	
	public SynthState(int i, BasicOscillator o, VCA v, SamplePlayer s, Color c) {
		super();
		stateID = i;
		
		osc = o;
		vca = v;
		player = s;
		
		//Initialize the rest of the key drawing data
		for(int j=0; j<xCoords.length; j++){
			if(heights[j] == heights[0]){//white key
				widths[j] = 60;
			}else{//black key
				widths[j] = 40;
			}
			//Fill in the upper octave
			if(xCoords[j] == 0)
				xCoords[j] = xCoords[j-12]+420;
			noteOn[j] = false;
		}
		for(int j=0;j<xCoords.length;j++)
			xCoords[j]+=xOffset;
		
		keyColor = c;
		
		tuning = BasicOscillator.EQUAL_TEMPERED;
		baseNote = 0;//Tuned to A by default
		
		table = new String[12][12][2];
		centIntervals = new double[12][12];
		
		displayCents = true;
		recalcTable();
		
		black = true;

	}
	
	public void init(GameContainer gc, StateBasedGame sbg) throws SlickException{		

	}
	
	@Override
	public void enter(GameContainer gc, StateBasedGame sbg) throws SlickException{
	
	}
	
	public void setBlack(boolean b){
		black = b;
	}
	
	
	@Override
	public void leave(GameContainer gc, StateBasedGame sbg) throws SlickException{
		//player.stopPlayer();
	}
	
	public void render(GameContainer gc, StateBasedGame sbg, Graphics g){
		if(!black){
			
		if(tuning == BasicOscillator.EQUAL_TEMPERED){
			g.setColor(new Color(30,30,30));
			g.fillRect(50,458,900,500);
			g.setColor(Color.white);
		}		
		
		//Coloured vertical bars over the baseNote
		for(int i=0; i<xCoords.length;i++){
			if(tuning!= BasicOscillator.EQUAL_TEMPERED && i%12 == baseNote){
				g.setColor(keyColor.darker(.5f));
				g.fillRect(xCoords[i], 42, widths[i], 800);
				g.setColor(Color.white);
			}
		}

		//Draw the white keys
		for(int i=0; i<xCoords.length; i++){
			if(heights[i]==heights[0]){
				if(noteOn[i]){
					g.setColor(keyColor.darker(.4f));
				}else{
					g.setColor(Color.black);
				}
				g.fillRect(xCoords[i], yCoord, widths[i], heights[i]);
				g.setColor(Color.white);
				g.drawRect(xCoords[i], yCoord, widths[i], heights[i]);
				g.drawString(keyNames[i], xCoords[i]+25, yCoord+120);
			}
		}
		
		//Draw the black keys
		for(int i=0; i<xCoords.length; i++){
			if(heights[i]==heights[1]){
				if(noteOn[i]){
					g.setColor(keyColor);
				}else{
					g.setColor(new Color(70,70,70));
				}
				g.fillRect(xCoords[i], yCoord, widths[i], heights[i]);
				g.setColor(Color.white);
				g.drawRect(xCoords[i], yCoord, widths[i], heights[i]);
				g.drawString(keyNames[i], xCoords[i]+15, yCoord+70);
			}
		}
		
		g.drawString("A440: "+(Math.round(osc.getA()*10.0)/10.0), 848, 678);
		
		g.drawString(tuningNames[tuning], 50, 678);
		
		//Draw the Ratios table. 13x13, each cell is 71x32 px
		
		//Draw the table background
		g.setColor(Color.black);
		g.fillRect(50, 42 , 60+12*70 , 416);

		//Draw the shading to show how different the interval is from the reference
		for(int i=0 ; i < table.length ; i++){
			for(int j=0 ; j < table[0].length; j++){
				double a = Math.round(centIntervals[i][j]);
				double b = Math.round(centIntervals[baseNote][j]);
				int c = (int)Math.abs(b-a) * 3;
				if( c != 0){
					g.setColor(new Color(c,c,c));
					g.fillRect((j)*71+98, (i+1)*32+42 , 70, 31);
				}
			}
		}
		g.setColor(Color.white);
		
		//Draw the lines of the table
		g.setColor(Color.gray);
		for(int i=42+32;i<=416+42;i+=32)
			g.drawLine(50, i, 50+48+12*71, i);
		for(int i=50+48;i<=98+71*12;i+=71)
			g.drawLine(i, 42, i, 458);
		g.setColor(Color.white);
		g.drawLine(50, 42+32, 98+71*12, 42+32);
		g.drawLine(50+48, 42, 50+48, 458);
		
		//Draw a horizontal bar through the baseNote
		if(tuning == BasicOscillator.EQUAL_TEMPERED){
			g.setColor(new Color(30,30,30));
		}else{
			g.setColor(keyColor.darker(.5f));
		}
		g.fillRect(0, baseNote*32 + 42+32+1, 1000, 31);
		g.setColor(Color.white);
		
		//Draw the headings
		for(int i=0; i<rowHeaders.length; i++)
			g.drawString(rowHeaders[i], 65, (i+1)*32+51);
		
		for(int i=0; i<colHeaders.length; i++)
			g.drawString(colHeaders[i], 125+i*71, 51);
		
		//Put the numbers in the table
		for(int i=0; i< table.length; i++)//i is rows, j is columns
			for(int j=0;j<table[0].length;j++){
				if(displayCents){
					g.drawString(table[i][j][0], (j)*71+101, (i+1)*32+51);
				}else{
					if(table[i][j][0].length() < 4){
						g.drawString(table[i][j][0]+"/"+table[i][j][1], (j)*71+101, (i+1)*32+51);					
					}else{
						g.drawString(table[i][j][0], (j)*71+108, (i+1)*32+42);
						g.drawLine((j)*71+105, (i+1)*32+58, (j)*71+155, (i+1)*32+58);
						g.drawString(table[i][j][1], (j)*71+108, (i+1)*32+58);
					}
				}
			}
		}
	}
	
	public void update(GameContainer gc, StateBasedGame sbg, int delta) throws SlickException {
		Input input = gc.getInput();
		if(input.isKeyPressed(Input.KEY_ESCAPE)){
			player.stopPlayer();
			try {Thread.sleep(100);} catch (InterruptedException e) {}
			gc.exit();
		}
		
//		if(input.isKeyPressed(Input.KEY_SPACE)){
//			for(int i=0;i<vca.state.length;i++)
//				System.out.println(vca.state[i]);
//		}
	}

	public void keyPressed(int arg0, char arg1) {
		//Regarding the value in arg0:
		//"1" is 2 and arg0 increases left to right along the keyboard row up to Backspace which is 14
		//Tab is 15, arg0 increases left to right until ] which is 27. Enter is 28 and Backslash is 43
		//Input.KEY_A is 30
		//Input.KEY_Z is 44
		if(arg0>=2 && arg0<=27 || arg0==43){//If a note key is pressed, activate the note.
			if(arg0 == 43){
				vca.noteOn(BasicOscillator.NUM_NOTES-1);
				noteOn[BasicOscillator.NUM_NOTES-1] = true;
			}else{
				if(keyMap[arg0]!=-1){
					vca.noteOn(keyMap[arg0]);
					noteOn[keyMap[arg0]] = true;
				}
			}
		}else if((arg0 >= 30 && arg0 <= 40) || arg0==Input.KEY_ENTER){//Changing the base note
			if(arg0==Input.KEY_ENTER)
				arg0 = 41;
			baseNote = arg0-30;
			osc.setTuning(tuning,baseNote);
			recalcTable();
		}else if(arg0 >= 44 && arg0 <= 48){//Changing the tuning system
			tuning = arg0 - 44;
			if(tuning == BasicOscillator.EQUAL_TEMPERED)
				displayCents = true;
			osc.setTuning(tuning, baseNote);
			//System.out.println(tuning);
			recalcTable();
		}else if(arg0 == Input.KEY_LSHIFT){//LSHIFT changes the display type of the table
			displayCents = !displayCents;
			if(tuning == BasicOscillator.EQUAL_TEMPERED)
				displayCents = true;
			recalcTable();
		}else if(arg0 == Input.KEY_SLASH){//Changes the highlight colour
			keyColor = new Color((int)(Math.random()*256),(int)( Math.random()*256), (int)(Math.random()*256));
		}else if(arg0 == Input.KEY_LEFT){//Changes the tuning system
			tuning = mod(tuning-1, osc.tunings.size());
			if(tuning == BasicOscillator.EQUAL_TEMPERED)
				displayCents = true;
			osc.setTuning(tuning,baseNote);
			recalcTable();
		}else if(arg0 == Input.KEY_RIGHT){//Changes the tuning system
			tuning = mod(tuning+1, osc.tunings.size());
			if(tuning == BasicOscillator.EQUAL_TEMPERED)
				displayCents = true;
			osc.setTuning(tuning,baseNote);
			recalcTable();
		}else if(arg0 == Input.KEY_UP){//Changes the base note
			baseNote = mod(baseNote-1, 12);
			osc.setTuning(tuning,baseNote);
			recalcTable();
		}else if(arg0 == Input.KEY_DOWN){//Changes the base note
			baseNote = mod(baseNote+1, 12);
			osc.setTuning(tuning,baseNote);
			recalcTable();
		}
	}

	public void keyReleased(int arg0, char arg1) {
		if(arg0>1 && arg0<=27 || arg0==43){//Release a note
			if(arg0 == 43){
				vca.noteOff(BasicOscillator.NUM_NOTES-1);
				noteOn[BasicOscillator.NUM_NOTES-1] = false;
			}else{
				if(keyMap[arg0]!=-1){
					vca.noteOff(keyMap[arg0]);
					noteOn[keyMap[arg0]] = false;
				}
			}
		}
	}
	
	@Override
	public int getID() {
		return stateID;
	}
	
	//Recalculates values for the ratios table
	public void recalcTable(){
		if(displayCents || tuning == BasicOscillator.EQUAL_TEMPERED){
			//Calculate the intervals in cents
			centIntervals = new double[12][12];
			double[] freqs = osc.getFrequencies();
			for(int i=0; i<centIntervals.length; i++){
				for(int j=0; j<centIntervals[0].length; j++){
					centIntervals[mod(i+baseNote,12)][j] = 1200.0 * Math.log(freqs[mod(i+j+baseNote,12)]/freqs[mod(i+baseNote,12)]) / Math.log(2.0);
					if(centIntervals[mod(i+baseNote,12)][j]<0)
						centIntervals[mod(i+baseNote,12)][j]+=1200;
				}
			}
			
			//Put centIntervals into table
			for(int i=0; i<centIntervals.length; i++){
				for(int j=0; j<centIntervals[0].length; j++){
					table[i][j][0] = ""+(int)Math.round(centIntervals[i][j]);
					table[i][j][1] = "";
				}
			}
		}else{
			//Calculate the intervals as fractions
			//Second ratio divided by first
			//Row determines offset from base note
			//Column determines interval step
			Fraction[][] f = new Fraction[12][12];
			Fraction[] intervals = osc.getIntervals(tuning);
			for(int i=0; i<f.length; i++){
				for(int j=0; j<f[0].length; j++){
					int factor = 1;
					if(i+j>=12)//If you've gone up an octave
						factor = 2;
					f[mod(i+baseNote,12)][j] = intervals[mod(i+j,12)].divide(intervals[i]).multiply(factor);
				}
			}
			
			//Copy f into the table
			for(int i=0; i<f.length; i++){
				for(int j=0; j<f[0].length; j++){
					table[i][j][0] = ""+f[i][j].getNumerator();
					table[i][j][1] = ""+f[i][j].getDenominator();
				}
			}
				//table[i][j] = ""+Math.round(100*f[i][j].doubleValue());
		}
		
	}
	
	private int mod(int x, int y)
	{
	    int result = x % y;
	    if (result < 0)
	    {
	        result += y;
	    }
	    return result;
	}
	
}
