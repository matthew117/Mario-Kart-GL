package bates.jamie.graphics.scene;

import static javax.media.opengl.GL.GL_BLEND;
import static javax.media.opengl.GL.GL_TEXTURE_2D;
import static javax.media.opengl.fixedfunc.GLLightingFunc.GL_LIGHTING;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GL2;

import bates.jamie.graphics.util.RGB;
import bates.jamie.graphics.util.Shader;
import bates.jamie.graphics.util.Vector;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.Texture;

public class IndexedModel implements Serializable
{
	private static final long serialVersionUID = 2138514666222530425L;

	private List<float[]> _vertices;
	
	private FloatBuffer vertices;
	private FloatBuffer normals;
	private FloatBuffer texCoords;
	private FloatBuffer colors;
	
	IntBuffer indices;
	
	int polygon;
	int indexCount;
	
	
	Texture texture;
	
	
	public static boolean enableVBO = true;
	public boolean bufferCreated = false;
	
	private int[] bufferIDs = new int[5];
	
	private static final int VERTEX_BUFFER = 0;
	private static final int NORMAL_BUFFER = 1;
	private static final int TCOORD_BUFFER = 2;
	private static final int COLOUR_BUFFER = 3;
	
	private static final int INDEX_BUFFER = 4;
	
	public IndexedModel(String fileName)
	{
		long start = System.currentTimeMillis();
		
		polygon = 3;
		load(fileName);
		
		System.out.println("Model Loader: " + fileName + ", " + (System.currentTimeMillis() - start));
	}
	
	
	public IndexedModel(List<float[]> vertices, List<float[]> normals, int[] vIndices, int[] nIndices, int type, boolean enableVBO)
	{
		if(type == 3) polygon = GL2.GL_TRIANGLES;
		if(type == 4) polygon = GL2.GL_QUADS;
		
		float[] _normals = reorderNormals(vertices.size(), normals, vIndices, nIndices);
		
		this.vertices = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		for(float[] vertex : vertices) this.vertices.put(vertex);
		this.vertices.position(0);  
		
		_vertices = vertices;
		
		indexCount = vIndices.length;
		
		this.normals = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		this.normals.put(_normals);
		this.normals.position(0);
		
		System.out.printf("Indexed Model:\n{\n\tIndices:  %d\n\tVertices: %d\n\tNormals:  %d\n}\n", indexCount, vertices.size(), normals.size());
		
		indices = Buffers.newDirectIntBuffer(vIndices);
	}
	
	public IndexedModel(List<float[]> vertices, List<float[]> normals, int[] vIndices, int[] nIndices, int type)
	{
		if(type == 3) polygon = GL2.GL_TRIANGLES;
		if(type == 4) polygon = GL2.GL_QUADS;
		
		List<float[]> vertexData = new ArrayList<float[]>();
		List<float[]> normalData = new ArrayList<float[]>();
		List<Integer> offsetData  = new ArrayList<Integer>();
		
		int index = 0;
		
		for(int i = 0; i < vIndices.length; i++)
		{
			float[] vertex = vertices.get(vIndices[i]);
			float[] normal = normals.get(nIndices[i]);
			
			boolean unique = true;
			int k = index;
			
			for(int j = 0; j < vertexData.size(); j++)
			{
				if( Vector.equal(vertex, vertexData.get(j)) &&
					Vector.equal(normal, normalData.get(j))    )
				{
					k = j;
					unique = false;
					break;
				}
			}
			
			if(unique)
			{
				offsetData.add(k);
				vertexData.add(vertex);
				normalData.add(normal);
				index++;
			}
			else offsetData.add(k);
		}
		
		_vertices = vertexData;
		
		indexCount = vIndices.length;
		
		this.vertices = Buffers.newDirectFloatBuffer(vertexData.size() * 3);
		for(float[] vertex : vertexData) this.vertices.put(vertex);
		this.vertices.position(0);  
		
		this.normals = Buffers.newDirectFloatBuffer(normalData.size() * 3);
		for(float[] normal : normalData) this.normals.put(normal);
		this.normals.position(0);
		
		indices = Buffers.newDirectIntBuffer(offsetData.size());
		for(int i : offsetData) indices.put(i);
		indices.position(0);
		
		System.out.printf("Indexed Model:\n{\n\tIndices:  %d\n\tVertices: %d\n\tNormals:  %d\n}\n", indexCount, vertices.size(), normals.size());
	}
	
	public boolean save(String fileName)
	{
        try
        {
            FileOutputStream   fos = new FileOutputStream(new File("obj/" + fileName + ".model"));
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            
            List<Float>   vertexData = new ArrayList<Float>();
            List<Float>   normalData = new ArrayList<Float>();
            List<Integer> offsetData = new ArrayList<Integer>();
            
            for(int i = 0; i < vertices.capacity(); i++) vertexData.add(vertices.get(i));
            for(int i = 0; i <  normals.capacity(); i++) normalData.add(normals.get(i));
            for(int i = 0; i <  indices.capacity(); i++) offsetData.add(indices.get(i));
            
            oos.writeObject(vertexData);
            oos.writeObject(normalData);
            oos.writeObject(offsetData);
            oos.writeObject(indexCount);
            
            oos.close();
            fos.close(); 
        }
        catch (IOException e) { e.printStackTrace(); }
        
        return true;
    }
	
	@SuppressWarnings("unchecked")
	public boolean load(String fileName)
	{
        try
        {
            FileInputStream   fis = new FileInputStream(new File("obj/" + fileName + ".model"));
            ObjectInputStream ois = new ObjectInputStream(fis);
            
            List<Float>   vertexData = (ArrayList<Float>)   ois.readObject();
            List<Float>   normalData = (ArrayList<Float>)   ois.readObject();
            List<Integer> offsetData = (ArrayList<Integer>) ois.readObject();
            indexCount = (Integer) ois.readObject();
            
            this.vertices = Buffers.newDirectFloatBuffer(vertexData.size() * 3);
    		for(float v : vertexData) this.vertices.put(v);
    		this.vertices.position(0);  
    		
    		this.normals = Buffers.newDirectFloatBuffer(normalData.size() * 3);
    		for(float n : normalData) this.normals.put(n);
    		this.normals.position(0);
    		
    		indices = Buffers.newDirectIntBuffer(offsetData.size());
    		for(int i : offsetData) indices.put(i);
    		indices.position(0);

            ois.close();
            fis.close();
        }
        catch (Exception e) { e.printStackTrace(); }
        return true;
    }
	
	public IndexedModel(List<float[]> vertices, int[] vIndices, int type)
	{
		if(type == 3) polygon = GL2.GL_TRIANGLES;
		if(type == 4) polygon = GL2.GL_QUADS;
		
		this.vertices = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		for(float[] vertex : vertices) this.vertices.put(vertex);
		this.vertices.position(0);  
		
		indexCount = vIndices.length;
		
		indices = Buffers.newDirectIntBuffer(vIndices);
	}
	
	public IndexedModel(List<float[]> vertices, List<float[]> texCoords, int[] vIndices, int[] tIndices, Texture texture, int type)
	{	
		this.texture = texture;
		
		if(type == 3) polygon = GL2.GL_TRIANGLES;
		if(type == 4) polygon = GL2.GL_QUADS;
		
		this.vertices = Buffers.newDirectFloatBuffer(vertices.size() * 3);
		for(float[] vertex : vertices) this.vertices.put(vertex);
		this.vertices.position(0);  
		
		indexCount = vIndices.length;
		
		this.texCoords = Buffers.newDirectFloatBuffer(vertices.size() * 2);
		for(float[] texCoord : texCoords) this.texCoords.put(texCoord);
		this.texCoords.position(0);
		
		System.out.printf("Indexed Model:\n{\n\tIndices:  %d\n\tVertices: %d\n\tTexture Coordinates: %d\n}\n", indexCount, vertices.size(), texCoords.size());
		
		indices = Buffers.newDirectIntBuffer(vIndices);
	}
	
	public void export(String fileName)
	{
		try
		{	
			FileWriter writer = new FileWriter("iva/" + fileName + ".iva");
			BufferedWriter out = new BufferedWriter(writer);
			
			out.write("vertices ");
			float[] vertexArray = vertices.array();
			for(int v = 0; v < vertexArray.length; v++) out.write(vertexArray[v] + " ");
			out.write("\r\n");
			
			out.write("normals ");
			float[] normalArray = vertices.array();
			for(int n = 0; n < normalArray.length; n++) out.write(normalArray[n] + " ");
			out.write("\r\n");
			
			out.write("indices ");
			float[] indexArray = vertices.array();
			for(int i = 0; i < indexArray.length; i++) out.write(indexArray[i] + " ");
			out.write("\r\n");
	
			out.close();
		}
		
		catch(Exception e)
		{
			System.err.println(e.getMessage());
		}
	}
	
	private float[] reorderNormals(int vertices, List<float[]> normals, int[] vIndices, int[] nIndices)
	{
		// each vertex has a normal that requires 3 components
		float[] _normals = new float[vertices * 3];
		
		// for each vertex
		for(int i = 0; i < nIndices.length; i++)
		{
			float[] normal = normals.get(nIndices[i]);
			
			int offset = vIndices[i] * 3;
			
			_normals[offset    ] = normal[0];
			_normals[offset + 1] = normal[1];
			_normals[offset + 2] = normal[2];
		}
		
		return _normals;
	}
	
	public float[] reorderTexCoords(int vertices, List<float[]> texCoords, int[] vIndices, int[] tIndices)
	{
		//each vertex has a texture coordinate that requires 2 components
		float[] _texCoords = new float[vertices * 2];
		
		for(int i = 0; i < vertices; i++)
		{
			float[] texCoord = texCoords.get(tIndices[i]);
			
			int offset = i * 2; // TODO this may be incorrect
			
			_texCoords[offset    ] = texCoord[0];
			_texCoords[offset + 1] = texCoord[1];
		}
		
		return _texCoords;
	}
	
	public List<float[]> getVertices() { return _vertices; }
	
	public void setColorArray(List<float[]> colors)
	{
		this.colors = Buffers.newDirectFloatBuffer(colors.size() * 3);
		for(float[] color : colors) this.colors.put(color);
		this.colors.position(0);
	}
	
	public void render(GL2 gl)
	{
		if(!bufferCreated && enableVBO) createBuffers(gl);
		
		if(texCoords == null) gl.glDisable(GL2.GL_TEXTURE_2D);
		else gl.glEnable(GL2.GL_TEXTURE_2D);
		
		if(texture != null) texture.bind(gl);
		
		enableState(gl);
		
		if(enableVBO)
		{
			bindArrays(gl);
			gl.glDrawElements(polygon, indexCount, GL2.GL_UNSIGNED_INT, 0);
			unbindArrays(gl);
		}
		else
		{
			setupPointers(gl);
			gl.glDrawElements(polygon, indexCount, GL2.GL_UNSIGNED_INT, indices);
		}
		
		disableState(gl);
		
		gl.glEnable(GL_TEXTURE_2D);	
	}

	private void unbindArrays(GL2 gl)
	{
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER        , 0);
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private void setupPointers(GL2 gl)
	{
								gl.glVertexPointer  (3, GL2.GL_FLOAT, 0, vertices );
		if(normals   != null)   gl.glNormalPointer  (   GL2.GL_FLOAT, 0, normals  );
		if(colors    != null)   gl.glColorPointer   (3, GL2.GL_FLOAT, 0, colors   );
		if(texCoords != null) { gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, texCoords); texture.bind(gl); }
	}

	private void bindArrays(GL2 gl)
	{
		                      gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[VERTEX_BUFFER]); gl.glVertexPointer  (3, GL2.GL_FLOAT, 0, 0);
		if(normals   != null) gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[NORMAL_BUFFER]); gl.glNormalPointer  (   GL2.GL_FLOAT, 0, 0);
		if(colors    != null) gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[COLOUR_BUFFER]); gl.glColorPointer   (3, GL2.GL_FLOAT, 0, 0);
		if(texCoords != null) gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[TCOORD_BUFFER]); gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);

		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, bufferIDs[INDEX_BUFFER]);
	}

	private void disableState(GL2 gl)
	{
							  gl.glDisableClientState(GL2.GL_VERTEX_ARRAY       );
		if(normals   != null) gl.glDisableClientState(GL2.GL_NORMAL_ARRAY       );
		if(texCoords != null) gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		if(colors    != null) gl.glDisableClientState(GL2.GL_COLOR_ARRAY        );
	}

	private void enableState(GL2 gl)
	{
		                      gl.glEnableClientState(GL2.GL_VERTEX_ARRAY       );
		if(normals   != null) gl.glEnableClientState(GL2.GL_NORMAL_ARRAY       );
		if(texCoords != null) gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		if(colors    != null) gl.glEnableClientState(GL2.GL_COLOR_ARRAY        );
	}
	
	public void renderInstanced(GL2 gl, int count, int dataID)
	{
		if(!bufferCreated && enableVBO) createBuffers(gl);
		
		if(texCoords == null) gl.glDisable(GL2.GL_TEXTURE_2D);
		else gl.glEnable(GL2.GL_TEXTURE_2D);
		
		gl.glActiveTexture(GL2.GL_TEXTURE1); gl.glBindTexture(GL2.GL_TEXTURE_1D, dataID);
		gl.glActiveTexture(GL2.GL_TEXTURE0); if(texture != null) texture.bind(gl);
		
		enableState(gl);
		
		if(enableVBO)
		{
			bindArrays(gl);
			gl.glDrawElementsInstanced(polygon, indexCount, GL2.GL_UNSIGNED_INT, null, count);
			unbindArrays(gl);
		}
		else
		{
			setupPointers(gl);
			gl.glDrawElementsInstanced(polygon, indexCount, GL2.GL_UNSIGNED_INT, indices, count);
		}
		
		disableState(gl);
		
		gl.glEnable(GL_TEXTURE_2D);	
	}
	
	public void renderNormals(GL2 gl, boolean smooth, float scale)
	{	
		Shader.disable(gl);
		
		gl.glDisable(GL2.GL_LIGHTING);
		gl.glDisable(GL2.GL_TEXTURE_2D);
		gl.glDisable(GL2.GL_TEXTURE_CUBE_MAP);
		
		float[] c = RGB.BLUE;
		gl.glColor3f(c[0]/255, c[1]/255, c[2]/255);

//		gl.glEnable(GL2.GL_BLEND);
//		gl.glEnable(GL2.GL_LINE_SMOOTH);
		gl.glHint(GL2.GL_LINE_SMOOTH_HINT, GL2.GL_NICEST);
		
		gl.glBegin(GL2.GL_LINES);
		
		for(int i = 0; i < _vertices.size(); i++)
		{
			float[] p1 = _vertices.get(i);
			float[] normal = new float[3]; normals.get(normal, 0, 3);
			float[] p2 = Vector.add(p1, Vector.multiply(normal, scale));
			
			gl.glVertex3f(p1[0], p1[1], p1[2]);
			gl.glVertex3f(p2[0], p2[1], p2[2]);
		}
		gl.glEnd();
		
		normals.position(0);
		
		gl.glDisable(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_POINT_SMOOTH);
		
		gl.glColor3f(1, 1, 1);
		
		gl.glEnable(GL2.GL_LIGHTING);
		gl.glEnable(GL_TEXTURE_2D);	
	}
	
	public void createBuffers(GL2 gl)
	{
		gl.glGenBuffers(5, bufferIDs, 0);
	    
	    // Vertex data
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[VERTEX_BUFFER]);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, Float.SIZE / 8 * vertices.capacity(), vertices, GL2.GL_STATIC_DRAW);
	    
	    // Normal data
		if(normals != null)
		{
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[NORMAL_BUFFER]);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, Float.SIZE / 8 * normals.capacity(), normals, GL2.GL_STATIC_DRAW);
		}
	    
	    // Texture coordinates
		if(texCoords != null)
		{
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[TCOORD_BUFFER]);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, Float.SIZE / 8 * texCoords.capacity(), texCoords, GL2.GL_STATIC_DRAW);
		}
		
		// Color data
		if(colors != null)
		{
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferIDs[COLOUR_BUFFER]);
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, Float.SIZE / 8 *  colors.capacity(), colors, GL2.GL_STATIC_DRAW);
		}
	    
	    // Indices
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, bufferIDs[INDEX_BUFFER]);
		gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, Integer.SIZE / 8 * indexCount, indices, GL2.GL_STATIC_DRAW);
		
		unbindArrays(gl);
		
		bufferCreated = true;
	}
	
	public void renderGlass(GL2 gl, float[] color)
	{
		gl.glDisable(GL_TEXTURE_2D);
		gl.glColor4f(color[0], color[1], color[2], 0.25f);
		
		gl.glDisable(GL_LIGHTING);
		gl.glEnable(GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		
		gl.glFrontFace(GL2.GL_CW ); render(gl);
		gl.glFrontFace(GL2.GL_CCW); render(gl);
		
		gl.glColor3f(1, 1, 1);
		gl.glEnable(GL_TEXTURE_2D);	
		
		gl.glDisable(GL_BLEND);
		gl.glEnable(GL_LIGHTING);
	}
}
