/*-
 * #%L
 * A nice project implementing an OMERO connection with ImageJ
 * %%
 * Copyright (C) 2021 EPFL
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package ch.epfl.biop.omero.imageloader;

import bdv.AbstractViewerSetupImgLoader;
import bdv.viewer.Source;
import ch.epfl.biop.bdv.bioformats.bioformatssource.BioFormatsBdvSource;
import ch.epfl.biop.bdv.bioformats.imageloader.BioFormatsSetupLoader;
import ch.epfl.biop.omero.omerosource.OmeroSource;
import ch.epfl.biop.omero.omerosource.OmeroSourceOpener;
import mpicbg.spim.data.generic.sequence.ImgLoaderHint;
import mpicbg.spim.data.sequence.MultiResolutionSetupImgLoader;
import mpicbg.spim.data.sequence.VoxelDimensions;
import net.imglib2.Dimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.AbstractIntegerType;
import net.imglib2.type.numeric.real.FloatType;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class OmeroSetupLoader<T extends NumericType<T>,V extends Volatile<T> & NumericType<V>> extends AbstractViewerSetupImgLoader<T, V> implements MultiResolutionSetupImgLoader< T > {

    public Source<T> concreteSource;
    public Source<V> volatileSource;

    //int[] cellDimensions;

    Function<RandomAccessibleInterval<T>, RandomAccessibleInterval<FloatType>> cvtRaiToFloatRai;

    final Converter<T,FloatType> cvt;

    Consumer<String> errlog = s -> System.err.println(BioFormatsSetupLoader.class+" error:"+s);


    final OmeroSourceOpener opener;
    final public int iChannel;

    public OmeroSetupLoader(OmeroSourceOpener opener,
                            int channelIndex,
                            T t,
                            V v) throws Exception {
        super(t, v);

        this.opener = opener;
        iChannel = channelIndex;

        List<Source> sources = opener.getConcreteAndVolatileSources(channelIndex);

        concreteSource = sources.get(0);
        volatileSource = sources.get(1);

        //cellDimensions = new int[]{opener.getTileSizeX(0),opener.getTileSizeY(0),1};

        if (t instanceof FloatType) {
            cvt = null;
            cvtRaiToFloatRai = rai -> (RandomAccessibleInterval<FloatType>) rai; // Nothing to be done
        }else if (t instanceof ARGBType) {
            // Average of RGB value
            cvt = (input, output) -> {
                int val = ((ARGBType) input).get();
                int r = ARGBType.red(val);
                int g = ARGBType.green(val);
                int b = ARGBType.blue(val);
                output.set(r+g+b);
            };
            cvtRaiToFloatRai = rai -> Converters.convert( rai, cvt, new FloatType());
        }else if (t instanceof AbstractIntegerType) {
            cvt = (input, output) -> output.set(((AbstractIntegerType) input).getRealFloat());
            cvtRaiToFloatRai = rai -> Converters.convert( rai, cvt, new FloatType());
        }else {
            cvt = null;
            cvtRaiToFloatRai = e -> {
                errlog.accept("Conversion of "+t.getClass()+" to FloatType unsupported.");
                return null;
            };
        }

    }

    //getters
    public Gateway getGateway(){ return opener.getGateway(); }
    public SecurityContext getSecurityContext(){ return opener.getSecurityContext(); }
    public Long getOmeroId(){ return opener.getOmeroId(); }

    @Override
    public RandomAccessibleInterval<V> getVolatileImage(int timepointId, int level, ImgLoaderHint... hints) {
        return volatileSource.getSource(timepointId,level);
    }

    @Override
    public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId, int level, boolean normalize, ImgLoaderHint... hints) {
        return cvtRaiToFloatRai.apply(getImage(timepointId,level));
    }

    @Override
    public Dimensions getImageSize(int timepointId, int level) {
        return new Dimensions() {
            @Override
            public void dimensions(long[] dimensions) {
                concreteSource.getSource(timepointId,level).dimensions(dimensions);
            }

            @Override
            public long dimension(int d) {
                return concreteSource.getSource(timepointId,level).dimension(d);
            }

            @Override
            public int numDimensions() {
                return concreteSource.getSource(timepointId,level).numDimensions();
            }
        };
    }

    @Override
    public RandomAccessibleInterval<T> getImage(int timepointId, int level, ImgLoaderHint... hints) {
        return concreteSource.getSource(timepointId,level);
    }

    @Override
    public double[][] getMipmapResolutions() {
        // Needs to compute mipmap resolutions... pfou
        int numMipmapLevels = concreteSource.getNumMipmapLevels();
        double [][] mmResolutions = new double[ numMipmapLevels ][3];
        mmResolutions[0][0]=1;
        mmResolutions[0][1]=1;
        mmResolutions[0][2]=1;

        RandomAccessibleInterval srcL0 = concreteSource.getSource(0,0);
        for ( int iLevel = 1; iLevel< numMipmapLevels; iLevel++) {
            RandomAccessibleInterval srcLi = concreteSource.getSource(0,iLevel);
            mmResolutions[iLevel][0] = (double)srcL0.dimension(0)/(double)srcLi.dimension(0);
            mmResolutions[iLevel][1] = (double)srcL0.dimension(1)/(double)srcLi.dimension(1);
            mmResolutions[iLevel][2] = (double)srcL0.dimension(2)/(double)srcLi.dimension(2);
        }
        return mmResolutions;
    }

    @Override
    public AffineTransform3D[] getMipmapTransforms() {
        AffineTransform3D[] ats = new AffineTransform3D[concreteSource.getNumMipmapLevels()];

        for (int iLevel = 0; iLevel< concreteSource.getNumMipmapLevels(); iLevel++) {
            AffineTransform3D at = new AffineTransform3D();
            concreteSource.getSourceTransform(0,iLevel,at);
            ats[iLevel] = at;
        }

        return ats;
    }

    @Override
    public int numMipmapLevels() {
        return concreteSource.getNumMipmapLevels();
    }

    @Override
    public RandomAccessibleInterval<FloatType> getFloatImage(int timepointId, boolean normalize, ImgLoaderHint... hints) {
        return cvtRaiToFloatRai.apply(getImage(timepointId,0));
    }

    @Override
    public Dimensions getImageSize(int timepointId) {
        return getImageSize(0,0);
    }

    @Override
    public VoxelDimensions getVoxelSize(int timepointId) {
        return concreteSource.getVoxelDimensions();
    }
}
