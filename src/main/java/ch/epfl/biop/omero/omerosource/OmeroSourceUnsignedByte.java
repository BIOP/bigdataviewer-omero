package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.ij2command.OmeroTools;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import omero.api.RawPixelsStorePrx;

import java.util.concurrent.ConcurrentHashMap;

public class OmeroSourceUnsignedByte extends OmeroSource<UnsignedByteType> {
    public OmeroSourceUnsignedByte(OmeroSourceOpener opener, int c) throws Exception {
        super(opener, c);
    }

    @Override
    public RandomAccessibleInterval<UnsignedByteType> createSource(int t, int level) {

        try {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            // Create cached image factory of Type Byte
            ReadOnlyCachedCellImgOptions options = new ReadOnlyCachedCellImgOptions();

            int sx = this.opener.getSizeX(level);
            int sy = this.opener.getSizeY(level);
            int sz = this.opener.getSizeZ(level);

            // Set cell dimensions according to level
            int xc = this.opener.getTileSizeX(level);
            int yc = this.opener.getTileSizeY(level);
            int zc = 1;

            // Creates cached image factory of Type Byte
            options = options.cellDimensions(xc,yc,zc);
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(options);

            // Creates image, with cell Consumer method, which creates the image
            final Img<UnsignedByteType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedByteType(),
                    cell -> {

                        try {
                            synchronized (OmeroTools.class) {
                                RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);
                                rawPixStore.setPixelsId(this.opener.getPixelsID(), false);

                                //setResolutionLevels indexes are in reverse order compared to the other methods
                                //here index 0 is the lowest resolution and n-1 is the highest
                                rawPixStore.setResolutionLevel(this.opener.getNLevels()-1-level);

                                Cursor<UnsignedByteType> out = Views.flatIterable(cell).cursor();

                                //cell connait sa position dans l'espace (dans la grande image)
                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, sx);

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, sy);

                                int w = maxX - minX;
                                int h = maxY - minY;

                                byte[] bytes = rawPixStore.getTile((int) cell.min(2), channel_index, t, minX, minY, w, h);

                                int idxPx = 0;
                                int totBytes = (w * h);

                                while ((out.hasNext()) && (idxPx < totBytes)) {
                                    out.next().set(bytes[idxPx]);
                                    idxPx++;
                                }
                                rawPixStore.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            raiMap.get(t).put(level, rai);
            return raiMap.get(t).get(level);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public UnsignedByteType getType() {
        return new UnsignedByteType();
    }

}
