package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.ij2command.OmeroTools;
import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;

import java.util.concurrent.ConcurrentHashMap;

public class OmeroSourceUnsignedShort extends OmeroSource<UnsignedShortType> {
    public OmeroSourceUnsignedShort(OmeroSourceOpener opener, int c) throws Exception {
        super(opener, c);
    }

    @Override
    public RandomAccessibleInterval<UnsignedShortType> createSource(int t, int level) {
        try {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            RandomAccessibleInterval<UnsignedShortType> rai = OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,level,ctx,gt);

            raiMap.get(t).put(level, rai);
            return raiMap.get(t).get(level);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }

}
