/* SVN FILE: $Id$ */
package se.ltu.android.demo.scene;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.FloatBuffer;

import se.ltu.android.demo.scene.intersection.AABBox;
import se.ltu.android.demo.scene.shapes.Box;
import se.ltu.android.demo.util.BufferUtils;

/**
 * An application specific extension of a Node representing a chess board.
 * @author Åke Svedin <ake.svedin@gmail.com>
 * @version $Revision$
 * @lastmodified $Date$
 */
public class Board extends Container {
	
	public Board(String name) {
		super(name);
		//pickBoxes = new PickBox[64];
		
		Box darkSquare = new Box("darkSquare", 1.0f, 1.0f, 0.2f);
		Box lightSquare = new Box("lightSquare", 1.0f, 1.0f, 0.2f);
		//darkSquare.setSolidColor(new float[]{0.4f,0.2f,0.08f,1.0f});
		//lightSquare.setSolidColor(new float[]{0.87f,0.62f,0.45f,1.0f});
		darkSquare.setSolidColor(new float[]{0.1f,0.1f,0.1f,1.0f});
		lightSquare.setSolidColor(new float[]{1.0f,1.0f,1.0f,1.0f});
		
		Object3D curSquare = null;
		PickBox pickBox = null;
		int oddeven = 1; // 1a (a dark square) starts as odd
		String squareName;
		// create chess board from 1a to 8h
		for(int row = 1; row < 9; row++) {
			for(int col = 0; col < 8; col++) {
				squareName = String.valueOf((char)('a'+col)) + row;
				if((oddeven & 1) == 0) {
					curSquare = lightSquare.cloneMesh(squareName);
				} else {
					curSquare = darkSquare.cloneMesh(squareName);
				}
				curSquare.setLocalTranslation(col-3.5f, row-4.5f, 0.0f);
				attachChild(curSquare);
				
				// create pickable box
				pickBox = new PickBox(squareName, new AABBox(-.5f, -.5f, -.1f, .5f, .5f, .1f));
				pickBox.setLocalTranslation(col-3.5f, row-4.5f, 0.0f);
				//pickBoxes[(row-1)*8 + col] = pickBox;
				attachChild(pickBox);
				
				oddeven++;
			}
			oddeven++;
		}
		mergeChildren("grid");
	}

	/**
	 * Merge all TriMeshes <i>directly attached</i> to this node to one single TriMesh
	 * This method removes 
	 * @param name name of the new TriMesh
	 */
	// TODO move to Node, set to public and make more general
	// TODO null pointer error checking
	// TODO only works on children with the same draw method (i.e. TRIANGLES).. no error checking!
	private void mergeChildren(String name) {
		int len = children.size();
		Spatial child;
		Object3D triChild;
		int nVertices = 0;
		int nIndices = 0;
		int nTexCoords = 0;
		int nNormals = 0;
		int nColors = 0;
		CharBuffer oIndices;
		int iOffset;
		
		// get the buffer sizes first
		for (int i = 0; i < len; i++) {
			child = children.get(i);
			if(child instanceof Object3D) {
				triChild = (Object3D)child;
				nVertices += triChild.mVertices.capacity();
				nIndices += triChild.mIndices.capacity();
				nTexCoords += triChild.mTexcoords.capacity();
				nNormals += triChild.mNormals.capacity();
				nColors += triChild.mColors.capacity();
			}
		}
		
		FloatBuffer vertices = BufferUtils.createFloatBuffer(nVertices);
		CharBuffer indices = BufferUtils.createCharBuffer(nIndices);
		FloatBuffer texcoords = BufferUtils.createFloatBuffer(nTexCoords);
		FloatBuffer normals = BufferUtils.createFloatBuffer(nNormals);
		ByteBuffer colors = BufferUtils.createByteBuffer(nColors);
		vertices.clear();
		indices.clear();
		texcoords.clear();
		normals.clear();
		colors.clear();
		
		// then fill our buffer
		for (int i = 0; i < len; i++) {
			child = children.get(i);
			if(child instanceof Object3D) {
				triChild = (Object3D)child;
				
				iOffset = (vertices.position()+1)/3;
				
				triChild.updateTransform();
				vertices.put(triChild.getWorldVertices());
				
				oIndices = triChild.mIndices;
				oIndices.clear();
				try {
					while(oIndices.hasRemaining()) {
						indices.put((char) (iOffset + oIndices.get()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				triChild.mTexcoords.clear();
				texcoords.put(triChild.mTexcoords);
				
				triChild.mNormals.clear();
				normals.put(triChild.mNormals);
				
				triChild.mColors.clear();
				colors.put(triChild.mColors);
				
				// remove directly attached TriMeshes
				children.remove(i);
				i--;
				len--;
			}
		}
			
		Object3D ret = new Object3D(name, vertices, indices);
		ret.mTexcoords = texcoords;
		ret.mNormals = normals;
		ret.mColors = colors;
		
		// create empty bb.. currently we need no more
		// TODO check bounding volumes during iteration
		ret.mModelBound = new AABBox();
		ret.setPickable(false);
		attachChild(ret);
	}
}