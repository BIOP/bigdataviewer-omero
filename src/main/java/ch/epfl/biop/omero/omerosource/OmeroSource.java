package ch.epfl.biop.omero.omerosource;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSourceRGB24bits;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSourceUnsignedByte;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSourceUnsignedShort;
import ch.epfl.biop.ij2command.OmeroTools;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import ome.model.units.BigResult;
import omero.api.IMetadataPrx;
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


public abstract class OmeroSource<T extends NumericType< T >> implements Source<T>{

    protected final DefaultInterpolators< T > interpolators = new DefaultInterpolators<>();

    int sizeT;
    int nLevels;
    final int channel_index;
    final long imageID;
    //Concurrent hash map allows different threads to work at the same time
    //raiMap: 1st key: time point // 2nd key: resolution level
    final Map<Integer,Map<Integer,RandomAccessibleInterval<T>>> raiMap = new ConcurrentHashMap<>();
    SecurityContext ctx;
    final Gateway gt;
    double pSizeX;
    double pSizeY;
    double pSizeZ;
    double stagePosX;
    double stagePosY;
    OmeroSourceOpener opener;
    boolean displayInSpace;
    boolean positionIgnoreBioFormatsMetaData;


    public OmeroSource(OmeroSourceOpener opener, int c){
        this.imageID = opener.omeroImageID;
        this.gt = opener.gateway;

        this.ctx = opener.securityContext;
        this.pSizeX = opener.getPixelSizeX();
        this.pSizeY = opener.getPixelSizeY();
        this.pSizeZ = opener.getPixelSizeZ();
        this.stagePosX = opener.getStagePosX();
        this.stagePosY = opener.getStagePosY();
        this.sizeT = opener.getSizeT();
        this.nLevels = opener.getNLevels();
        this.channel_index = c;
        this.opener = opener;
        this.displayInSpace = opener.displayInSpace;
        this.positionIgnoreBioFormatsMetaData = opener.positionIgnoreBioFormatsMetaData;
    }

    @Override
    public boolean isPresent(int t) {
        return t<sizeT;
    }
    /*
    @Override
    synchronized public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
        if (!map.containsKey(t)){
            try {
            map.put(t,OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,level,ctx,gt));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return map.get(t);
    }*/


    /**
     * The core function of the source which is implemented in subclasses
     * @see BioFormatsBdvSourceRGB24bits
     * @see BioFormatsBdvSourceUnsignedByte
     * @see BioFormatsBdvSourceUnsignedShort
     * @param t // timepoint
     * @param level // resolution level
     * @return
     */
    abstract public RandomAccessibleInterval<T> createSource(int t, int level);

    /**
     * Returns stored RAI of requested timepoint and resolution level
     * @param t
     * @param level
     * @return
     */
    @Override
    public RandomAccessibleInterval<T> getSource(int t, int level) {
        //TODO: check if synchronized is required
        if (raiMap.containsKey(t)) {
            if (raiMap.get(t).containsKey(level)) {
                return raiMap.get(t).get(level);
            }
        }
        return createSource(t,level);
    }

    @Override
    public RealRandomAccessible<T> getInterpolatedSource(int t, int level, Interpolation method) {
        final T zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<T, RandomAccessibleInterval< T >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< T > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        transform.identity();
        if(positionIgnoreBioFormatsMetaData){
        transform.scale((double)opener.imageSize.get(0)[0]/(double)opener.imageSize.get(level)[0],
                (double)opener.imageSize.get(0)[1]/(double)opener.imageSize.get(level)[1],
                (double)opener.imageSize.get(0)[2]/(double)opener.imageSize.get(level)[2]);
        } else {
            transform.scale(pSizeX * (double) opener.imageSize.get(0)[0] / (double) opener.imageSize.get(level)[0],
                    pSizeY * (double) opener.imageSize.get(0)[1] / (double) opener.imageSize.get(level)[1],
                    pSizeZ * (double) opener.imageSize.get(0)[2] / (double) opener.imageSize.get(level)[2]);
            if (this.displayInSpace == true) {
                transform.translate(new double[]{stagePosX, stagePosY, 0});
            }
        }
    }

    @Override
    abstract public T getType();

    @Override
    public String getName() {
        return "OMERO ID: "+imageID+ "; Channel: "+ channel_index;
    }

    @Override
    public VoxelDimensions getVoxelDimensions() { return new VoxelDimensions() {
        @Override
        public String unit() {
            return "pixel";
        }

        @Override
        public void dimensions(double[] doubles) {
            doubles[0] = 1;
            doubles[1] = 1;
            doubles[2] = 1;
        }

        @Override
        public double dimension(int i) {
            return 1;
        }

        @Override
        public int numDimensions() {
            return 3;
        }
    };
    }

    @Override
    public int getNumMipmapLevels() {
        return nLevels;
    }
}