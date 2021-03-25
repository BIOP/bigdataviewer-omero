package ch.epfl.biop.ij2command;

import bdv.tools.transformation.TransformedSource;
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

public class OmeroSource implements Source<UnsignedShortType>{

    protected final DefaultInterpolators< UnsignedShortType > interpolators = new DefaultInterpolators<>();

    final int timepoints;
    final int channel;
    TransformedSource ts;


    public OmeroSource(int t, int channel_index){

        this.timepoints = t;
        this.channel = channel_index;
    }

    @Override
    public boolean isPresent(int t) {
        return false;
    }

    @Override
    public RandomAccessibleInterval<UnsignedShortType> getSource(int t, int level) {
        return null;
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
        transform.scale();
    }

    @Override
    public UnsignedShortType getType() {
        UnsignedShortType type = new UnsignedShortType();
        return type;
    }

    @Override
    public String getName() {
        return "hello";
    }

    @Override
    public VoxelDimensions getVoxelDimensions() {
        return null;
    }

    @Override
    public int getNumMipmapLevels() {
        return 1;
    }
}