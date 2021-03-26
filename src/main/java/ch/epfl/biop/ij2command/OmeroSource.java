package ch.epfl.biop.ij2command;

import bdv.util.DefaultInterpolators;
import bdv.viewer.Interpolation;
import bdv.viewer.Source;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.ExtendedRandomAccessibleInterval;
import net.imglib2.view.Views;
import ome.model.units.BigResult;
import omero.gateway.Gateway;
import omero.gateway.model.PixelsData;
import omero.model.enums.UnitsLength;

public class OmeroSource implements Source<UnsignedShortType>{

    protected final DefaultInterpolators< UnsignedShortType > interpolators = new DefaultInterpolators<>();

    int sizeT;
    final int channel_index;
    PixelsData pixels;
    final Gateway gateway;


    public OmeroSource(int t, int c, PixelsData px, Gateway gt){
        this.sizeT = t;
        this.channel_index = c;
        this.pixels = px;
        this.gateway = gt;

    }

    @Override
    public boolean isPresent(int t) {
        return false;
    }

    @Override
    public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
        System.out.println("getSource");
        try {
            return OmeroTools.openRawRandomAccessibleInterval(gateway,pixels,t,channel_index);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public RealRandomAccessible<UnsignedShortType> getInterpolatedSource(int t, int level, Interpolation method) {
        System.out.println("getinterpSource");
        final UnsignedShortType zero = getType();
        zero.setZero();
        ExtendedRandomAccessibleInterval<UnsignedShortType, RandomAccessibleInterval< UnsignedShortType >>
                eView = Views.extendZero(getSource( t, level ));
        RealRandomAccessible< UnsignedShortType > realRandomAccessible = Views.interpolate( eView, interpolators.get(method) );
        return realRandomAccessible;
    }

    @Override
    public void getSourceTransform(int t, int level, AffineTransform3D transform) {
        System.out.println("getSourceTransform");
        try {
            double pSizeX = pixels.getPixelSizeX(UnitsLength.MILLIMETER).getValue();
            double pSizeY = pixels.getPixelSizeX(UnitsLength.MILLIMETER).getValue();
            double pSizeZ = pixels.getPixelSizeX(UnitsLength.MILLIMETER).getValue();
            transform.scale(pSizeX, pSizeY,pSizeZ);
        } catch (BigResult bigResult) {
            bigResult.printStackTrace();
        }
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
        System.out.println("getVoxDim");
        return null;
    }

    @Override
    public int getNumMipmapLevels() {
        return 1;
    }
}