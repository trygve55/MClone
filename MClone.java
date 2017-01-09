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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Random;

//import demos.common.TextureReader;

class Renderer extends GLCanvas implements GLEventListener {
	private GLU glu;
	FPSAnimator fPSAnimator;
	public LocalMap localMap;
	KeyboardInput keyboardInput = new KeyboardInput();
	MouseInput mouseInput = new MouseInput();	
	
	Player player = new Player(0, 6, 0);
	
	boolean physics = true;
	
	int deltaT, timer, selectedBlock = 2;
	
	long lastTime = System.nanoTime();
	private float cameraX = 0, cameraY = 6, cameraZ = 0, cameraDirUp = 0, cameraDirSide = 0, mouseTimerRemove = 0.0f, mouseTimerPlace = 0.0f;; 
	
	int[] look; 
	
	private short[] textures = new short[1];
	
	Renderer(LocalMap localMap) {
		this.addGLEventListener(this);
		this.addKeyListener(keyboardInput);
		this.addMouseListener(mouseInput);
		this.localMap = localMap;
		player.setMap(localMap);
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
		
		processKeyboardInput(deltaT);
		processMouseInput();
		player.renderTick(deltaT);
		updateCamera();
		
		draw3D(gl);
		drawHUD(gl);
		
		if (timer == 60) {
			System.out.println("FPS: " + (int) (1000000.0f/ (float) deltaT) + "  render time: " + (System.nanoTime() - lastTime)/1000000 + "ms   x: " + cameraX + " y: " + cameraY + " z: " + cameraZ);
			//System.out.println(localMap.getBlock(-38, 2, 33));
			
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
		
		gl.glTranslatef((float) -localMap.getLocalMapSize()/2 + 1, 0.0f, -localMap.getLocalMapSize()/2 + 1);
		
		for (int x = -localMap.getLocalMapSize()/2+1; x < localMap.getLocalMapSize()/2; x++) {
			for (int y = 0; y < localMap.getMapHeight(); y++) {
				for (int z = -localMap.getLocalMapSize()/2+1; z < localMap.getLocalMapSize()/2; z++) {
					// if (localMap.getBlock(x, y, z) != 0 && (localMap.getBlock(x + 1, y, z) == 0 || localMap.getBlock(x - 1, y, z) == 0 || localMap.getBlock(x, y + 1, z) == 0 || localMap.getBlock(x, y - 1, z) == 0 || localMap.getBlock(x, y, z + 1) == 0 || localMap.getBlock(x, y, z - 1) == 0)) {
						// drawBlock(gl, localMap.getBlock(x, y, z));
					// }
					if (look != null && x == look[0] && y == look[1] && z == look[2]) {
						drawBlock(gl, 3, 
						(cameraX > x && localMap.getBlock(x + 1, y, z) == 0), 
						(cameraX < x && localMap.getBlock(x - 1, y, z) == 0), 
						(cameraY > y && localMap.getBlock(x, y + 1, z) == 0),
						(cameraY < y && localMap.getBlock(x, y - 1, z) == 0), 
						(cameraZ > z && localMap.getBlock(x, y, z + 1) == 0), 
						(cameraZ < z && localMap.getBlock(x, y, z - 1) == 0));
					} else if (localMap.getBlock(x, y, z) != 0 &&
						(localMap.getBlock(x + 1, y, z) == 0 ||
						localMap.getBlock(x - 1, y, z) == 0 ||
						localMap.getBlock(x, y + 1, z) == 0 ||
						localMap.getBlock(x, y - 1, z) == 0 ||
						localMap.getBlock(x, y, z + 1) == 0 ||
						localMap.getBlock(x, y, z - 1) == 0)) {
						
						drawBlock(gl, localMap.getBlock(x, y, z),
						(cameraX > x && localMap.getBlock(x + 1, y, z) == 0),
						(cameraX < x && localMap.getBlock(x - 1, y, z) == 0), 
						(cameraY > y && localMap.getBlock(x, y + 1, z) == 0),
						(cameraY < y && localMap.getBlock(x, y - 1, z) == 0),
						(cameraZ > z && localMap.getBlock(x, y, z + 1) == 0),
						(cameraZ < z && localMap.getBlock(x, y, z - 1) == 0));
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
	
	private void updateCamera() {
		float[] camera = player.getEyePosition(); // {x, y, x, sideDir, upDir}
		cameraX = camera[0];
		cameraY = camera[1];
		cameraZ = camera[2];
		
		glu.gluLookAt(camera[0], camera[1], camera[2], camera[0] + (Math.sin(Math.toRadians(-camera[3]))) * Math.cos(Math.toRadians(-camera[4])), camera[1] + Math.sin(Math.toRadians(-camera[4])), camera[2] + (Math.cos(Math.toRadians(-camera[3]))) * Math.cos(Math.toRadians(-camera[4])), 0, 1, 0);	
	}
	
	private void processKeyboardInput(int deltaT) {
		
		float[] speed = player.getSpeed();
		
		float moveSpeed = 0.09f;  //move speed
		// float speedX = speed[0], speedY = speed[1], speedZ = speed[2];
		float speedX = 0.0f, speedY = speed[1], speedZ = 0.0f;
		
		int forwardKey = KeyEvent.VK_W;
		int backwardKey = KeyEvent.VK_S;
		int leftKey = KeyEvent.VK_A;
		int rightKey = KeyEvent.VK_D;
		int upKey = KeyEvent.VK_SHIFT;
		int downKey = KeyEvent.VK_CONTROL;
		int jumpKey = KeyEvent.VK_SPACE;
		
		//input
		
		if (keyboardInput.keyHold(forwardKey)) {
			speedX += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.sin(Math.toRadians(-cameraDirSide));
			if (physics == false) speedY -= ((float) deltaT/16600.0f) * moveSpeed * Math.sin(Math.toRadians(cameraDirUp));
			speedZ += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.cos(Math.toRadians(-cameraDirSide));
		}
		
		if (keyboardInput.keyHold(backwardKey)) {
			speedX += -((float) deltaT/16600.0f) * moveSpeed * (float) Math.sin(Math.toRadians(-cameraDirSide));
			if (physics == false) speedY += ((float) deltaT/16600.0f) * moveSpeed * Math.sin(Math.toRadians(cameraDirUp));
			speedZ += -((float) deltaT/16600.0f) * moveSpeed * (float) Math.cos(Math.toRadians(-cameraDirSide));
		}
		
		if (keyboardInput.keyHold(leftKey)) {
			speedX += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.sin(Math.toRadians(-cameraDirSide + 90));
			speedZ += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.cos(Math.toRadians(-cameraDirSide + 90));
		}
		
		if (keyboardInput.keyHold(rightKey)) {
			speedX += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.sin(Math.toRadians(-cameraDirSide - 90));
			speedZ += ((float) deltaT/16600.0f) * moveSpeed * (float) Math.cos(Math.toRadians(-cameraDirSide - 90));
		}
		
		if (keyboardInput.keyHold(upKey)) {
			if (physics == false) speedY += ((float) deltaT/16600.0f) * moveSpeed;
		}
		
		if (keyboardInput.keyHold(downKey)) {
			if (physics == false) speedY -= ((float) deltaT/16600.0f) * moveSpeed;
		}
		
		if (keyboardInput.keyHold(jumpKey)) {
			if (player.isOnGround()) speedY = 0.18f;
		}
		
		player.setSpeed(speedX, speedY, speedZ);
	}
		
	private void processMouseInput() {
		
		//mouse movement and camera rotation
		float sence = 0.1f;
		
		double[] mouseChange = mouseInput.getMouseMovement();
		
		cameraDirUp += (float) mouseChange[1]*sence;
		cameraDirSide += (float) mouseChange[0]*sence;
		
		float cameraAngleLimit = 89.9f;
		
		if (cameraDirUp > cameraAngleLimit)  cameraDirUp = cameraAngleLimit;
		if (cameraDirUp < -cameraAngleLimit)  cameraDirUp = -cameraAngleLimit;
		
		player.setLookDir(cameraDirSide, cameraDirUp);
		
		look = getLookAtBlock();
		
		//mouse bottons
		if (mouseTimerRemove > 0.0f) {
			mouseTimerRemove -= 16.0f * ((float) deltaT/16600.0f);
		} else if (mouseTimerRemove <= 0.0f && look != null && mouseInput.buttonHold(1)) {
			localMap.setBlock(look[0], look[1], look[2], 0);
			mouseTimerRemove = 180.0f;
		}
		
		if (mouseTimerPlace > 0.0f) {
			mouseTimerPlace -= 16.0f * ((float) deltaT/16600.0f);
		} else if (mouseTimerPlace <= 0.0f && look != null && mouseInput.buttonHold(3)) {
			localMap.setBlock(look[0], look[1], look[2], selectedBlock);
			mouseTimerPlace = 180.0f;
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
	
	private void drawBlock(GL2 gl, int type, boolean drawPositiveX, boolean drawNegativeX, boolean drawPositiveY, boolean drawNegativeY, boolean drawPositiveZ, boolean drawNegativeZ) {
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
			if (drawPositiveZ) {
				gl.glVertex3f(scale, scale, scale);
				gl.glVertex3f(-scale, scale, scale);
				gl.glVertex3f(-scale, -scale, scale);
				gl.glVertex3f(scale, -scale, scale);
			}
			//left
			if (drawPositiveX) {
				gl.glVertex3f(scale, scale, -scale);
				gl.glVertex3f(scale, scale, scale);
				gl.glVertex3f(scale, -scale, scale);
				gl.glVertex3f(scale, -scale, -scale);
			}
			//rigth
			if (drawNegativeX) {
				gl.glVertex3f(-scale, scale, scale);
				gl.glVertex3f(-scale, scale, -scale);
				gl.glVertex3f(-scale, -scale, -scale);
				gl.glVertex3f(-scale, -scale, scale);
			}
			//top
			if (drawPositiveY) {
				gl.glVertex3f(scale, scale, -scale);
				gl.glVertex3f(-scale, scale, -scale);
				gl.glVertex3f(-scale, scale, scale);
				gl.glVertex3f(scale, scale, scale);
			}
			//botton
			if (drawNegativeY) {
				gl.glVertex3f(scale, -scale, -scale);
				gl.glVertex3f(-scale, -scale, -scale);
				gl.glVertex3f(-scale, -scale, scale);
				gl.glVertex3f(scale, -scale, scale);
			}
			//back
			if (drawNegativeZ) {
				gl.glVertex3f(-scale, scale, -scale);
				gl.glVertex3f(scale, scale, -scale);
				gl.glVertex3f(scale, -scale, -scale);
				gl.glVertex3f(-scale, -scale, -scale);
			}
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
	
	public void setPhysics(boolean physics) {
		this.physics = physics;
	}
	
	public boolean getPhysics() {
		return physics;
	}

	public int[] getLookAtBlock() {
		float[] camera = player.getEyePosition(); // {x, y, x, sideDir, upDir}
		cameraX = camera[0];
		cameraY = camera[1];
		cameraZ = camera[2];
		
		int lookAtX, lookAtY, lookAtZ; 
		
		for (float f = 0.0f; f < 6.0f; f += 0.2f) {
			lookAtX = (int) Math.round(camera[0] + f * (Math.sin(Math.toRadians(-camera[3]))) * Math.cos(Math.toRadians(-camera[4])));
			lookAtY = (int) Math.round(camera[1] + f * Math.sin(Math.toRadians(-camera[4])));
			lookAtZ = (int) Math.round(camera[2] + f * (Math.cos(Math.toRadians(-camera[3]))) * Math.cos(Math.toRadians(-camera[4])));
			//System.out.println(lookAtX + " " + lookAtY + " " + lookAtZ);
			
			if (localMap.getBlock(lookAtX, lookAtY, lookAtZ) != 0) {
				System.out.println(lookAtX + " " + lookAtY + " " + lookAtZ);
				return new int[] {lookAtX, lookAtY, lookAtZ};
			}
		}
		
		return null;
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
		
		spawnTree( -2, 3, -3);
		spawnTree( 5, 3, -8);
		
		// setBlock(10, 3, 4, 3);
		// setBlock(13, 3, 5, 3);
		// setBlock(10, 3, 6, 3);
		// setBlock(10, 3, 7, 3);
		// setBlock(10, 3, 8, 3);
		// setBlock(11, 3, 4, 3);
		// setBlock(12, 3, 4, 3);
		// setBlock(13, 3, 4, 3);
		// setBlock(11, 3, 8, 3);
		// setBlock(12, 3, 8, 3);
		// setBlock(13, 3, 8, 3);
		// setBlock(13, 3, 7, 3);
		// setBlock(13, 3, 6, 3);
		
		// setBlock(10, 4, 4, 3);
		// setBlock(13, 4, 5, 3);
		// setBlock(10, 4, 6, 3);
		// setBlock(10, 4, 7, 3);
		// setBlock(10, 4, 8, 3);
		// setBlock(11, 4, 4, 3);
		// setBlock(12, 4, 4, 3);
		// setBlock(13, 4, 4, 3);
		// setBlock(11, 4, 8, 3);
		// setBlock(12, 4, 8, 3);
		// setBlock(13, 4, 8, 3);
		// setBlock(13, 4, 7, 3);
		// setBlock(13, 4, 6, 3);
		
		// setBlock(10, 5, 4, 3);
		// setBlock(13, 5, 5, 3);
		// setBlock(10, 5, 6, 3);
		// setBlock(10, 5, 7, 3);
		// setBlock(10, 5, 8, 3);
		// setBlock(11, 5, 4, 3);
		// setBlock(12, 5, 4, 3);
		// setBlock(13, 5, 4, 3);
		// setBlock(11, 5, 8, 3);
		// setBlock(12, 5, 8, 3);
		// setBlock(13, 5, 8, 3);
		// setBlock(13, 5, 7, 3);
		// setBlock(13, 5, 6, 3);
		
	}
	
	public int getBlock(int blockX, int blockY, int blockZ) {
		if (blockX >= -getLocalMapSize()/2+1 && blockX <= getLocalMapSize()/2 && blockZ >= -getLocalMapSize()/2+1 && blockZ <= getLocalMapSize()/2 && blockY >=	 0 && blockY < getMapHeight() - 1) {
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
	
	public void spawnTree(int x, int y, int z) {
		setBlock(x, y, z, 4);
		setBlock(x, y + 1, z, 4);
		setBlock(x, y + 2, z, 4);
		setBlock(x - 1, y + 2, z, 5);
		setBlock(x + 1, y + 2, z, 5);
		setBlock(x, y + 2, z + 1, 5);
		setBlock(x, y + 2, z - 1, 5);
		setBlock(x, y + 3, z, 5);
	}
}

class KeyboardInput extends KeyAdapter {
	//Renderer renderer;
	
	boolean[] isPressed = new boolean[255];
	
	public KeyboardInput() {
		//this.renderer = renderer;
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

class MouseInput extends MouseAdapter {
	
	private double lastMouseX = MouseInfo.getPointerInfo().getLocation().getX(), lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
	
	boolean[] isPressed = new boolean[16];
	
	public double[] getMouseMovement() {
		double mouseChangeX = MouseInfo.getPointerInfo().getLocation().getX() - lastMouseX;
		double mouseChangeY = MouseInfo.getPointerInfo().getLocation().getY() - lastMouseY;
		
		try {
			Robot bot = new Robot();
			bot.mouseMove(500, 500);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		lastMouseX = MouseInfo.getPointerInfo().getLocation().getX();
		lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
		
		return new double[] {mouseChangeX, mouseChangeY};
	}
	
	public void mousePressed(MouseEvent e) {
		isPressed[e.getButton()] = true;
	}
	
	public void mouseReleased(MouseEvent e) {
		isPressed[e.getButton()] = false;
	}
	
	public boolean buttonHold(int buttonCode) {
		return isPressed[buttonCode];
	}
}

class Player {
	
	LocalMap localMap;
	
	boolean physics = true;
	
	float x, y, z, speedX, speedY, speedZ, lookDirSide, lookDirUp, friction, gravity;
	
	public Player(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.friction = 0.08f;
		this.gravity = 0.1f;
	}
	
	public float[] getPosition() {
		return new float[] {x, y, z};
	}
	
	public float[] getEyePosition() {
		return new float[] {x, y + 1.2f, z, lookDirSide, lookDirUp};
	}
	
	public void renderTick(int deltaT) {
		physics(deltaT);
	}
	
	public void setMap(LocalMap localMap) {
		this.localMap = localMap;
	}
	
	public void physics(int deltaT) {
		
		//check if posible to move player
		if (this.speedX > 0.0f && 
		localMap.getBlock(Math.round(x + 0.15f + this.speedX * ((float) deltaT/16600.0f)), Math.round(y + 0.49f), Math.round(z)) == 0 &&
		localMap.getBlock(Math.round(x + 0.15f + this.speedX * ((float) deltaT/16600.0f)), Math.round(y + 1.49f), Math.round(z)) == 0 ||
		this.speedX < 0.0f && 
		localMap.getBlock(Math.round(x - 0.15f + this.speedX * ((float) deltaT/16600.0f)), Math.round(y + 0.49f), Math.round(z)) == 0 &&
		localMap.getBlock(Math.round(x - 0.15f + this.speedX * ((float) deltaT/16600.0f)), Math.round(y + 1.49f), Math.round(z)) == 0)
		x += this.speedX;
		
		if (localMap.getBlock(Math.round(x), Math.round(y + 0.49f + this.speedY * ((float) deltaT/16600.0f)), Math.round(z)) == 0) y += this.speedY;
		
		if (this.speedZ > 0.0f &&
		localMap.getBlock(Math.round(x), Math.round(y + 0.49f), Math.round(z + 0.15f + this.speedZ * ((float) deltaT/16600.0f))) == 0 &&
		localMap.getBlock(Math.round(x), Math.round(y + 1.49f), Math.round(z + 0.15f + this.speedZ * ((float) deltaT/16600.0f))) == 0 ||
		this.speedZ < 0.0f &&
		localMap.getBlock(Math.round(x), Math.round(y + 0.49f), Math.round(z - 0.15f + this.speedZ * ((float) deltaT/16600.0f))) == 0 &&
		localMap.getBlock(Math.round(x), Math.round(y + 1.49f), Math.round(z - 0.15f + this.speedZ * ((float) deltaT/16600.0f))) == 0)
		z += this.speedZ;
		
		//System.out.println(isOnGround() + " " + x + " " + y + " " + z);
		//System.out.println(speedX + " " + speedY + " " + speedZ);
		
		//calculate next move
		if (speedX == 0.0f || (speedX > 0.0f && speedX - friction < 0.0f) || (speedX < 0.0f && speedX - friction > 0.0f)) speedX = 0.0f;
		else if (speedX > 0.0f) speedX -= friction * ((float) deltaT/16600.0f);
		else speedX += friction * ((float) deltaT/16600.0f);

		if (physics == true) {
			if (!isOnGround()) {
				speedY -= 0.0115f * ((float) deltaT/16600.0f);
			} else {
				speedY = 0;
				y = Math.round(y);
			}
		} else {
			if (speedY == 0 || (speedY > 0 && speedY - friction < 0) || (speedY < 0 && speedY - friction > 0)) speedY = 0; 
			else if (speedY > 0) speedY -= friction * ((float) deltaT/16600.0f);
			else speedY += friction * ((float) deltaT/16600.0f);
		}
		
		if (speedZ == 0 || (speedZ > 0 && speedZ - friction < 0) || (speedZ < 0 && speedZ - friction > 0)) speedZ = 0; 
		else if (speedZ > 0) speedZ -= friction * ((float) deltaT/16600.0f);
		else speedZ += friction * ((float) deltaT/16600.0f);
		
		this.speedX = speedX;
		this.speedY = speedY;
		this.speedZ = speedZ;
	}
	
	public void setSpeed(float speedX, float speedY, float speedZ) {
		this.speedX = speedX;
		this.speedY = speedY;
		this.speedZ = speedZ;
	}
	
	public float[] getSpeed() {
		return new float[] {speedX, speedY, speedZ};
	}
	
	public void setLookDir(float lookDirSide, float lookDirUp) {
		this.lookDirSide = lookDirSide;
		this.lookDirUp = lookDirUp;
	}
	
	public void setSpeed(float speed, float moveDirSide) {
		
	}
	
	public void move(float deltaX, float deltaY, float deltaZ) {
		
	}
	
	public void setPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public void setPos(int x, int y, int z, float lookDirSide, float lookDirUp) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.lookDirSide = lookDirSide;
		this.lookDirUp = lookDirUp;
	}	
	
	public boolean isOnGround() {
		return (localMap.getBlock(Math.round(x), Math.round(y - 0.51f), Math.round(z)) != 0 );
	}
}

class MClone {
	public static void main(String[] args) {
		String title = "MClone";
		int width = 1800, height = 1000;
		
		LocalMap map = new LocalMap(64, 64);
		
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