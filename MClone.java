import java.awt.*;
import javax.swing.*;
import java.lang.Math.*;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.FPSAnimator;
import java.io.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Random;

//import demos.common.TextureReader;

class Renderer extends GLCanvas implements GLEventListener {
	private GLU glu;
	FPSAnimator fPSAnimator;
	private LocalMap localMap;
	KeyboardInput keyboardInput = new KeyboardInput(this);
	
	boolean physics = true;
	
	int deltaT;
	
	int timer = 0;
	
	long lastTime = System.nanoTime();
	private float cameraX = 0, cameraY = 6, cameraZ = 0, cameraDirUp = 0, cameraDirSide = 0;
	private double lastMouseX = MouseInfo.getPointerInfo().getLocation().getX(), lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
	
	private short[] textures = new short[1];
	
	Renderer(LocalMap localMap) {
		this.addGLEventListener(this);
		this.addKeyListener(keyboardInput);
		this.localMap = localMap;
	}
	
	public void cameraSet(float x, float y, float z) {
		cameraX = x;
		cameraY = y;
		cameraZ = z;
	}
	
	public void cameraMove(float x, float y, float z) {
		cameraX += x;
		cameraY += y;
		cameraZ += z;
	}
	
	public void init(GLAutoDrawable glDrawable) {
		GL2 gl = glDrawable.getGL().getGL2();
		glu = new GLU();
		gl.glClearColor(0.2f, 0.6f, 0.8f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
		
		fPSAnimator = new FPSAnimator(glDrawable, 120);
		fPSAnimator.start();
	}
	
	public void reshape(GLAutoDrawable glDrawable, int x, int y, int width, int height) {
		GL2 gl = glDrawable.getGL().getGL2();
		
		if (height == 0) height = 1;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0, (float) width/height, 0.1, 100);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	public void display(GLAutoDrawable glDrawable) {
		GL2 gl = glDrawable.getGL().getGL2();
		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();
		
		this.deltaT = getDeltaT();
		
		updateLookDirection();
		moveCamera();
		draw3D(gl);
		drawHUD(gl);
		
		if (timer == 60) {
			System.out.println("FPS: " + 1000000.0f/ (float) deltaT + "  render time: " + (System.nanoTime() - lastTime)/1000000 + "ms");
			timer = 0;
		} else {
			timer++;
		}
	}
	
	private void draw3D(GL2 gl) {
		gl.glEnable(GL2.GL_DEPTH_TEST);
		
		drawLocalMap(gl);
	}
	
	private void drawHUD(GL2 gl) {
		gl.glLoadIdentity();
		//gl.glOrtho(0, width(), 0, height(),0,1);
		gl.glDisable(GL2.GL_DEPTH_TEST);
	}
	
	private void drawLocalMap(GL2 gl) {
		
		gl.glTranslatef((float) -localMap.getLocalMapSize()/2, 0.0f, -localMap.getLocalMapSize()/2);
		
		for (int x = -localMap.getLocalMapSize()/2+1; x < localMap.getLocalMapSize()/2; x++) {
			for (int y = 0; y < localMap.getMapHeight(); y++) {
				for (int z = -localMap.getLocalMapSize()/2+1; z < localMap.getLocalMapSize()/2; z++) {
					if (localMap.getBlock(x, y, z) != 0) {
						drawBlock(gl, localMap.getBlock(x, y, z));
					}
					gl.glTranslatef(0.0f, 0.0f, 1.0f);
				}
				gl.glTranslatef(0.0f, 1.0f, -localMap.getLocalMapSize()+1);
			}
			gl.glTranslatef(1.0f, -localMap.getMapHeight(), 0.0f);
		}
	}
	
	public void dispose(GLAutoDrawable glDrawable) {
		
	}
	
	private int getDeltaT() {
		long now = System.nanoTime();
		long deltaT = (now-lastTime)/1000;
		lastTime = now;
		return (int) deltaT;
	}
	
	private void updateLookDirection() {
		float sence = 0.1f;
		
		double mouseChangeX = MouseInfo.getPointerInfo().getLocation().getX() - lastMouseX;
		double mouseChangeY = MouseInfo.getPointerInfo().getLocation().getY() - lastMouseY;
		
		cameraDirUp += (float) mouseChangeY*sence;
		cameraDirSide += (float) mouseChangeX*sence;
		
		if (cameraDirUp > 90.0f)  cameraDirUp = 90.0f;
		if (cameraDirUp < -90.0f)  cameraDirUp = -90.0f;
		
		try {
			Robot bot = new Robot();
			bot.mouseMove(500, 500);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		lastMouseX = MouseInfo.getPointerInfo().getLocation().getX();
		lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
		
		glu.gluLookAt(cameraX, cameraY, cameraZ, cameraX + Math.sin(Math.toRadians(-cameraDirSide)), cameraY + Math.sin(Math.toRadians(-cameraDirUp)), cameraZ + Math.cos(Math.toRadians(-cameraDirSide)), 0, 1, 0);	
	}
	
	private void moveCamera() {
		float speed = 0.09f;
		
		int forwardKey = KeyEvent.VK_W;
		int backwardKey = KeyEvent.VK_S;
		int leftKey = KeyEvent.VK_A;
		int rightKey = KeyEvent.VK_D;
		int upKey = KeyEvent.VK_SHIFT;
		int downKey = KeyEvent.VK_CONTROL;
		int jumpKey = KeyEvent.VK_SPACE;
		
		//input
		
		if (keyboardInput.keyHold(forwardKey)) {
			cameraX += ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(-cameraDirSide));
			if (physics == false) cameraY -= ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(cameraDirUp));
			cameraZ += ((float) deltaT/16600.0f) * speed * Math.cos(Math.toRadians(-cameraDirSide));
		}
		
		if (keyboardInput.keyHold(backwardKey)) {
			cameraX -= ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(-cameraDirSide));
			if (physics == false) cameraY += ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(cameraDirUp));
			cameraZ -= ((float) deltaT/16600.0f) * speed * Math.cos(Math.toRadians(-cameraDirSide));
		}
		
		if (keyboardInput.keyHold(leftKey)) {
			cameraX += ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(-cameraDirSide + 90));
			cameraZ += ((float) deltaT/16600.0f) * speed * Math.cos(Math.toRadians(-cameraDirSide + 90));
		}
		
		if (keyboardInput.keyHold(rightKey)) {
			cameraX += ((float) deltaT/16600.0f) * speed * Math.sin(Math.toRadians(-cameraDirSide - 90));
			cameraZ += ((float) deltaT/16600.0f) * speed * Math.cos(Math.toRadians(-cameraDirSide - 90));
		}
		
		if (keyboardInput.keyHold(upKey)) {
			cameraY += ((float) deltaT/16600.0f) * speed;
		}
		
		if (keyboardInput.keyHold(downKey)) {
			cameraY -= ((float) deltaT/16600.0f) * speed;
		}
		
		if (keyboardInput.keyHold(jumpKey)) {
			cameraY += ((float) deltaT/16600.0f) * 0.3f;
		}
		
		//physics
		if(!isOnGround()) {
			cameraY -= ((float) deltaT/16600.0f) * 0.2f;
		}
	}
	
	public boolean isOnGround() {
		if (localMap.getBlock(Math.round(cameraX), Math.round(cameraY) - 2, Math.round(cameraZ)) != 0 ) {
			return true;
		} else {
			return false;
		}
	}
	
	private void drawBlock(GL2 gl, int type) {
		float scale = 0.5f;
		//drawBlock
		Random rand = new Random();
		
		//select color
		switch (type) {
			case 1:		gl.glColor3f(0.1f, 0.7f, 0.0f);	break;
			
			case 2:		gl.glColor3f(0.5f, 0.5f, 0.5f);	break;
			
			case 3:		gl.glColor3f(0.5f, 0.5f, 0.3f);	break;
			
			case 4:		gl.glColor3f(0.3f, 0.3f, 0.15f);	break;
			
			case 5:		gl.glColor3f(0.0f, 0.3f, 0.0f);	break;
			
			default: 	break;
		}
		//draw cube
		gl.glBegin(GL2.GL_QUADS);
			
			//front
			gl.glVertex3f(scale, scale, scale);
			gl.glVertex3f(-scale, scale, scale);
			gl.glVertex3f(-scale, -scale, scale);
			gl.glVertex3f(scale, -scale, scale);
			//left
			gl.glVertex3f(scale, scale, -scale);
			gl.glVertex3f(scale, scale, scale);
			gl.glVertex3f(scale, -scale, scale);
			gl.glVertex3f(scale, -scale, -scale);
			//rigth
			gl.glVertex3f(-scale, scale, scale);
			gl.glVertex3f(-scale, scale, -scale);
			gl.glVertex3f(-scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, scale);
			//top
			gl.glVertex3f(scale, scale, -scale);
			gl.glVertex3f(-scale, scale, -scale);
			gl.glVertex3f(-scale, scale, scale);
			gl.glVertex3f(scale, scale, scale);
			//botton
			gl.glVertex3f(scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, scale);
			gl.glVertex3f(scale, -scale, scale);
			//back
			gl.glVertex3f(-scale, scale, -scale);
			gl.glVertex3f(scale, scale, -scale);
			gl.glVertex3f(scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, -scale);
		gl.glEnd();
		
		//draw outline
		gl.glColor3f(0.0f, 0.0f, 0.0f);
		//front
		gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex3f(scale, scale, scale);
			gl.glVertex3f(-scale, scale, scale);
			gl.glVertex3f(-scale, -scale, scale);
			gl.glVertex3f(scale, -scale, scale);
		gl.glEnd();
		//back
		gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex3f(-scale, scale, -scale);
			gl.glVertex3f(scale, scale, -scale);
			gl.glVertex3f(scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, -scale);
		gl.glEnd();
		//sides
		gl.glBegin(GL2.GL_LINES);
			gl.glVertex3f(-scale, scale, -scale);
			gl.glVertex3f(-scale, scale, scale);
			gl.glVertex3f(scale, scale, -scale);
			gl.glVertex3f(scale, scale, scale);
			gl.glVertex3f(scale, -scale, -scale);
			gl.glVertex3f(scale, -scale, scale);
			gl.glVertex3f(-scale, -scale, -scale);
			gl.glVertex3f(-scale, -scale, scale);
		gl.glEnd();
	}
}

class Map {
	private Chunk[][] chunks;
	
	public Map() {
		int maxSize = 4;
		int genSize = 2;
		
		chunks = new Chunk[maxSize][maxSize];
		
		for (int chunkX = maxSize/2 - genSize; chunkX < maxSize/2 + genSize; chunkX++) {
			for (int chunkY = maxSize/2 - genSize; chunkY < maxSize/2 + genSize; chunkY++) {
				chunks[chunkX][chunkY] = new Chunk();
			}
		}	
	}
	
	public Chunk getChunk(int chunkX, int chunkY) {
		return chunks[chunkX][chunkY];
	}
	
	public int getMapWidthChunks() {
		return chunks.length;
	}
	
	public int getMapHeightChunks() {
		return chunks[0].length;
	}
}

class Chunk {
	private short[][][] blocks;
	
	public Chunk() {
		blocks = new short[16][128][16];
		
		//map generator
		
		for (short x = 0; x < blocks.length;x++) {
			for (short z = 0; z < blocks[0][0].length;z++) {
				blocks[x][2][z] = 1;
				blocks[x][1][z] = 3;
				blocks[x][0][z] = 2;
			}
		}
		setBlock(5, 3, 4, (short) 2);
	}
	
	public short getBlock(short blockX, short blockY, short blockZ) {
		return blocks[blockX][blockY][blockZ];
	}
	
	public boolean setBlock(int blockX, int blockY, int blockZ, short type) {
		blocks[blockX][blockY][blockZ] = type;
		return true;
	}
}

class LocalMap {
	private int[][][] blocks;
	
	public LocalMap(int localMapSize, int mapHeight) {
		blocks = new int[localMapSize][mapHeight][localMapSize];
		
		//map generator
		
		for (short x = 0; x < blocks.length;x++) {
			for (short z = 0; z < blocks[0][0].length;z++) {
				blocks[x][2][z] = 1;
				blocks[x][1][z] = 3;
				blocks[x][0][z] = 2;
			}
		}
		setBlock(5, 3, 4, 2);
		setBlock(-2, 3, -3, 4);
		setBlock(-2, 4, -3, 4);
		setBlock(-2, 5, -3, 4);
		setBlock(-3, 5, -3, 5);
		setBlock(-1, 5, -3, 5);
		setBlock(-2, 5, -2, 5);
		setBlock(-2, 5, -4, 5);
		setBlock(-2, 6, -3, 5);
	}
	
	public int getBlock(int blockX, int blockY, int blockZ) {
		if (blockX >= getLocalMapSize()/2-1 || blockX <= getLocalMapSize()/2 || blockZ >= getLocalMapSize()/2-1 || blockZ <= getLocalMapSize()/2 || blockY >= 0 || blockY < getMapHeight()) {
		return blocks[getLocalMapSize()/2-1+blockX][blockY][getLocalMapSize()/2-1+blockZ];	
		} else {
		return 0;
		}
	}
	
	public boolean setBlock(int blockX, int blockY, int blockZ, int type) {
		blocks[getLocalMapSize()/2-1+blockX][blockY][getLocalMapSize()/2-1+blockZ] = type;
		return true;
	}
	
	public int getLocalMapSize() {
		return blocks.length;
	}
	
	public int getMapHeight() {
		return blocks[0][0].length;
	}
}

class KeyboardInput extends KeyAdapter{
	Renderer renderer;
	
	boolean[] isPressed = new boolean[255];
	
	public KeyboardInput(Renderer renderer) {
		this.renderer = renderer;
	}
	
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		isPressed[keyCode] = true;		
	}
	
	public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
		isPressed[keyCode] = false;
	}
	
	public boolean keyHold(int keyCode) {
		return isPressed[keyCode];
	}
}

class Player {
	
	float x, y, z;
	
	public Player(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public float[] getPosition() {
		return new float[] {x, y, z};
	}
	
	public void move(float deltaX, float deltaY, float deltaZ) {
		
	}
	
	public void moveTo(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
}

class MClone {
	public static void main(String[] args) {
		String title = "MClone";
		int width = 1800, height = 1000;
		
		LocalMap map = new LocalMap(32, 64);
		
		GLCanvas canvans = new Renderer(map);
		canvans.setPreferredSize(new Dimension(width, height));
		
		final JFrame frame = new JFrame();
		frame.getContentPane().add(canvans);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setTitle(title);
		frame.pack();
		frame.setVisible(true);
	}
}