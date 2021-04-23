package ch.epfl.biop.omero.omerosource;

import IceInternal.Ex;
import bdv.util.volatiles.SharedQueue;
import ch.epfl.biop.bdv.bioformats.BioFormatsMetaDataHelper;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvOpener;
import ch.epfl.biop.ij2command.OmeroTools;
import net.imglib2.FinalInterval;
import net.imglib2.realtransform.AffineTransform3D;
import ome.units.UNITS;
import ome.units.unit.Unit;
import omero.api.RawPixelsStorePrx;
import omero.api.ResolutionDescription;
import omero.gateway.SecurityContext;
import omero.gateway.Gateway;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.model.Length;
import omero.model.enums.UnitsLength;

/**
 * Contains parameters that explain how to open all channel sources from an Omero Image
 */
public class OmeroSourceOpener {

    public OmeroSourceOpener() {
    }

    // All serializable fields
    long omeroImageID;
    // Channels options
    boolean splitRGBChannels = false;
    // Unit used for display
    public UnitsLength u;
    // Size of the blocks
    public FinalInterval cacheBlockSize = new FinalInterval(new long[]{0, 0, 0}, new long[]{512, 512, 1}); // needs a default size for z
    // Bioformats location fix
    public double[] positionPreTransformMatrixArray;
    public double[] positionPostTransformMatrixArray;


    // All non-serializable fields
    transient SharedQueue cc = new SharedQueue(2, 4);
    transient Gateway gateway;
    transient SecurityContext securityContext;
    transient int sizeX, sizeY, sizeZ;
    transient int sizeT;
    transient int sizeC;
    transient int nLevels;
    transient double psizeX;
    transient double psizeY;
    transient double psizeZ;


    // All get methods
    public int getSizeX() {
        return this.sizeX;
    }
    public int getSizeY() {
        return this.sizeY;
    }
    public int getSizeZ() {
        return this.sizeZ;
    }
    public int getSizeT() {
        return this.sizeT;
    }
    public int getSizeC() {
        return this.sizeC;
    }
    public int getNLevels() {
        return this.nLevels;
    }

    public double getPixelSizeX() {
        return this.psizeX;
    }
    public double getPixelSizeY(){
        return this.psizeY;
    }
    public double getPixelSizeZ(){
        return this.psizeZ;
    }

    //define image ID
    public OmeroSourceOpener imageID(long imageID) {
        this.omeroImageID = imageID;
        return this;
    }

    public OmeroSourceOpener splitRGBChannels() {
        splitRGBChannels = true;
        return this;
    }

    //define unit
    public OmeroSourceOpener unit(UnitsLength u) {
        this.u = u;
        return this;
    }

    public OmeroSourceOpener millimeter() {
        this.u = UnitsLength.MILLIMETER;
        return this;
    }

    public OmeroSourceOpener micrometer() {
        this.u = UnitsLength.MICROMETER;
        return this;
    }

    public OmeroSourceOpener nanometer() {
        this.u = UnitsLength.NANOMETER;
        return this;
    }

    // define gateway
    public OmeroSourceOpener gateway(Gateway gateway) {
        this.gateway = gateway;
        return this;
    }

    // define security context
    public OmeroSourceOpener securityContext(SecurityContext ctx) {
        this.securityContext = ctx;
        return this;
    }

    // define size fields based on omero image ID, gateway and security context
    public OmeroSourceOpener create() throws Exception {
        PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(omeroImageID, gateway, securityContext);
        RawPixelsStorePrx rawPixStore = gateway.getPixelsStore(securityContext);
        this.sizeX = pixels.getSizeX();
        this.sizeY = pixels.getSizeY();
        this.sizeZ = pixels.getSizeZ();
        this.sizeT = pixels.getSizeT();
        this.sizeC = pixels.getSizeC();
        this.nLevels = rawPixStore.getResolutionLevels();
        this.psizeX = pixels.getPixelSizeX(this.u).getValue();
        this.psizeY = pixels.getPixelSizeY(this.u).getValue();
        //to handle 2D images
        this.psizeZ = 1;
        Length length = pixels.getPixelSizeZ(this.u);
        if(length != null){
            this.psizeZ = length.getValue();
        }
        return this;
    }


    // All space transformation methods
    public OmeroSourceOpener flipPositionXYZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionX() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(-1,1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionY() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,-1,1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }

    public OmeroSourceOpener flipPositionZ() {
        if (this.positionPreTransformMatrixArray == null) {
            positionPreTransformMatrixArray = new AffineTransform3D().getRowPackedCopy();
        }
        AffineTransform3D at3D = new AffineTransform3D();
        at3D.set(positionPreTransformMatrixArray);
        at3D.scale(1,1,-1);
        positionPreTransformMatrixArray = at3D.getRowPackedCopy();
        return this;
    }


}
