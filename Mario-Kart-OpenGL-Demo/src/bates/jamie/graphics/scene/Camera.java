package bates.jamie.graphics.scene;
import static bates.jamie.graphics.util.Vector.multiply;
import static bates.jamie.graphics.util.Vector.subtract;

import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_MODELVIEW;
import static javax.media.opengl.fixedfunc.GLMatrixFunc.GL_PROJECTION;

import javax.media.opengl.GL2;
import javax.media.opengl.glu.GLU;

//TODO Zoom camera in when obscured by an obstacle

public class Camera extends AnchorPoint
{
	private CameraMode mode = CameraMode.DYNAMIC_VIEW;
	
	private float zoom = 0.75f;
	
	private boolean rearview = false;
	
	public Camera() {}
	
	public CameraMode getMode() { return mode; }
	
	public void setRearview(boolean mirror) { rearview = mirror; }
	
	public void setupView(GL2 gl, GLU glu, float[] p, float ry)
	{
		switch(mode)
		{	
			//Cause the camera to follow the car dynamically as it moves along the track 
			case DYNAMIC_VIEW:
			{	
				gl.glTranslatef(0, -15.0f * zoom, -30.0f * zoom);
				gl.glRotated(ry + (rearview ? 180 : 0), 0.0f, -1.0f, 0.0f);
	
				glu.gluLookAt(p[0], p[1], p[2],
							  p[0] - 10, p[1], p[2],
							  0, 1, 0);

				break;
			}
			//Focus the camera on the centre of the track from a bird�s eye view
			case BIRDS_EYE_VIEW:
			{
				gl.glMatrixMode(GL_PROJECTION);
				gl.glLoadIdentity();
				gl.glOrtho(-200, 200, -200, 200, 1, 200);
				glu.gluLookAt(0, 150, 0,
					          0, 0, 0,
					          0, 0, 1);
				gl.glMatrixMode(GL_MODELVIEW);
				gl.glLoadIdentity();

				break;
			}
			//Setup the camera to view the scene from the driver's perspective
			case DRIVERS_VIEW:
			{	
				gl.glTranslatef(0, -3.0f, 0);
				gl.glRotated(ry, 0.0f, -1.0f, 0.0f);
				
				glu.gluLookAt(p[0], p[1], p[2],
							  p[0] - 10, p[1], p[2],
					          0, 1, 0);
				
				break;
			}
			case FREE_LOOK_VIEW:
			{
				float[]   c = getPosition();
				float[][] u = getRotationMatrix();
				
				p = subtract(c, multiply(u[0], 20));
				
				glu.gluLookAt(c[0], c[1], c[2],
						      p[0], p[1], p[2],
				              u[1][0], u[1][1], u[1][2]);
			}
			
			default: break;
		}
	}
	
	public boolean isDynamic()     { return mode == CameraMode.DYNAMIC_VIEW;   }
	public boolean isAerial()      { return mode == CameraMode.BIRDS_EYE_VIEW; }
	public boolean isFirstPerson() { return mode == CameraMode.DRIVERS_VIEW;   }
	public boolean isFree()        { return mode == CameraMode.FREE_LOOK_VIEW; }
	
	public void cycleMode()
	{
		mode = CameraMode.cycle(mode);
		
		if(isFree()) reset();
	}
	
	public enum CameraMode
	{
		DYNAMIC_VIEW,
		BIRDS_EYE_VIEW,
		DRIVERS_VIEW,
		FREE_LOOK_VIEW;
		
		public static CameraMode cycle(CameraMode mode)
		{
			return values()[(mode.ordinal() + 1) % values().length];
		}
	}
}