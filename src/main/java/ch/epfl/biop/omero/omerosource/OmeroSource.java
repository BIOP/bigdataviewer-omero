package ch.epfl.biop.omero.omerosource;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import ch.epfl.biop.ij2command.OmeroTools;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import ome.model.units.BigResult;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.facility.RawDataFacility;
import omero.gateway.model.PixelsData;
import omero.model.enums.UnitsLength;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;


public class OmeroSource implements Source<UnsignedShortType>{

    protected final DefaultInterpolators< UnsignedShortType > interpolators = new DefaultInterpolators<>();

    int sizeT;
    int nLevels;
    final int channel_index;
    final long imageID;
    //final Map<Integer,RandomAccessibleInterval<UnsignedShortType>> map = new HashMap<>();
    final Map<Integer,RandomAccessibleInterval<UnsignedShortType>> map = new ConcurrentHashMap<>();
    SecurityContext ctx;
    final Gateway gt;
    double pSizeX;
    double pSizeY;
    double pSizeZ;

    public OmeroSource(OmeroSourceOpener opener, int c) throws Exception {
        //PixelsData pixels = OmeroTools.getPixelsDataFromOmeroID(imageID,gateway,ctx);
        //System.out.println("pixel type " + pixels.getPixelType());
        this.imageID = opener.omeroImageID;
        this.gt = opener.gateway;
        this.ctx = opener.securityContext;
        this.pSizeX = opener.getPixelSizeX();
        this.pSizeY = opener.getPixelSizeY();
        this.pSizeZ = opener.getPixelSizeZ();
        this.sizeT = opener.getSizeT();
        this.nLevels = opener.getNLevels();
        this.channel_index = c;
    }

    @Override
    public boolean isPresent(int t) {
        return t<sizeT;
    }

    @Override
    synchronized public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
        if (!map.containsKey(t)){
            try {
            map.put(t,OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,ctx,gt));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map.get(t);
    }

    @Override
    public RealRandomAccessible<UnsignedShortType> getInterpolatedSource(int t, int level, Interpolation method) {
        final UnsignedShortType zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<UnsignedShortType, RandomAccessibleInterval< UnsignedShortType >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< UnsignedShortType > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        transform.scale(pSizeX, pSizeY,pSizeZ);
    }

    @Override
    public UnsignedShortType getType() {
        UnsignedShortType type = new UnsignedShortType();
        return type;
    }

    @Override
    public String getName() {
        return "3D display of my OMERO image";
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return null;
    }

    @Override
    public int getNumMipmapLevels() {
        return nLevels;
    }

    public int getSizeT(){
        return sizeT;
    }
}