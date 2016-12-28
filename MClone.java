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
	private Map map;
	
	private float cameraX = 0, cameraY = 6, cameraZ = 15, cameraDirUp = 0, cameraDirSide = 0;
	private double lastMouseX = MouseInfo.getPointerInfo().getLocation().getX(), lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
	
	private short[] textures = new short[1];
	
	Renderer(Map map) {
		this.addGLEventListener(this);
		this.addKeyListener(new Input(this));
		this.map = map;
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
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);
		
		fPSAnimator = new FPSAnimator(glDrawable, 60);
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
		
		//update look direction
		updateLookDirection();	
		
		for (int chunkX = 0; chunkX < 1; chunkX++) {
			for (int chunkY = 0; chunkY < 1; chunkY++) {
				for (short x = 0; x < 16; x++) {
					for (short y = 0; y < 128; y++) {
						for (short z = 0; z < 16; z++) {
							if (map.getChunk(chunkX, chunkY).getBlock(x, y, z) != 0) {
								drawBlock(gl, map.getChunk(chunkX, chunkY).getBlock(x, y, z));
							}
							gl.glTranslatef(0.0f, 0.0f, 1.0f);
						}
						gl.glTranslatef(0.0f, 1.0f, -16.0f);
					}
					gl.glTranslatef(1.0f, -128.0f, 0.0f);
				}
				gl.glTranslatef(-16.0f, 0.0f, 0.0f);
			}
		}		
	}
	
	public void dispose(GLAutoDrawable glDrawable) {
		
	}
	
	private void updateLookDirection() {
		float sence = 0.08f;
		
		double mouseChangeX = MouseInfo.getPointerInfo().getLocation().getX() - lastMouseX;
		double mouseChangeY = MouseInfo.getPointerInfo().getLocation().getY() - lastMouseY;
		
		cameraDirUp += (float) mouseChangeY*sence;
		cameraDirSide += (float) mouseChangeX*sence;
		
		//System.out.println("x: " + (MouseInfo.getPointerInfo().getLocation().getX() - lastMouseX) + " y: " + (MouseInfo.getPointerInfo().getLocation().getY() - lastMouseY));
		
		try {
			Robot bot = new Robot();
			bot.mouseMove(500, 500);
		} catch (Exception e) {
			System.out.println(e);
		}
		
		lastMouseX = MouseInfo.getPointerInfo().getLocation().getX();
		lastMouseY = MouseInfo.getPointerInfo().getLocation().getY();
		
		glu.gluLookAt(cameraX, cameraY, cameraZ, cameraX + Math.sin(Math.toRadians(-cameraDirSide)), cameraY - Math.sin(Math.toRadians(cameraDirUp)), cameraZ + Math.cos(Math.toRadians(-cameraDirSide)), 0, 1, 0);	
	}
	
	private void drawBlock(GL2 gl, short type) {
		float scale = 0.5f;
		//drawBlock
		Random rand = new Random();
		
		//select color
		switch (type) {
			case 1:		gl.glColor3f(0.1f, 0.7f, 0.0f);	break;
			
			case 2:		gl.glColor3f(0.5f, 0.5f, 0.5f);	break;
			
			case 3:		gl.glColor3f(0.5f, 0.5f, 0.3f);	break;
			
			default: 	break;
		}
		// gl.glColor3f(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
		
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
	}
}

class Map {
	private Chunk[][] chunks;
	
	public Map() {
		chunks = new Chunk[1][1];
		
		chunks[0][0] = new Chunk();
	}
	
	public Chunk getChunk(int chunkX, int chunkY) {
		return chunks[chunkX][chunkY];
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
	}
	
	public short getBlock(short blockX, short blockY, short blockZ) {
		return blocks[blockX][blockY][blockZ];
	}
}

class Input extends KeyAdapter{
	Renderer renderer;
	
	public Input(Renderer renderer) {
		this.renderer = renderer;
	}
	
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();  
		switch(keyCode) {
			case KeyEvent.VK_UP:	renderer.cameraMove(0.0f, 0.0f, 1.0f); break;
			case KeyEvent.VK_DOWN:	renderer.cameraMove(0.0f, 0.0f, -1.0f); break;
			case KeyEvent.VK_RIGHT:	renderer.cameraMove(1.0f, 0.0f, 0.0f); break;
			case KeyEvent.VK_LEFT:	renderer.cameraMove(-1.0f, 0.0f, 0.0f); break;
			case KeyEvent.VK_X:		renderer.cameraMove(0.0f, 1.0f, 0.0f); break;
			case KeyEvent.VK_Z:		renderer.cameraMove(0.0f, -1.0f, 0.0f); break;
		}
		//CarvansOpenGL.this.repaint();
	}	
}

class Player {
	
}

class MClone {
	public static void main(String[] args) {
		String title = "SOL";
		int width = 1800, height = 1000;
		
		Map map = new Map();
		
		GLCanvas carvans = new Renderer(map);
		carvans.setPreferredSize(new Dimension(width, height));
		
		final JFrame frame = new JFrame();
		frame.getContentPane().add(carvans);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setTitle(title);
		frame.pack();
		frame.setVisible(true);
	}
}