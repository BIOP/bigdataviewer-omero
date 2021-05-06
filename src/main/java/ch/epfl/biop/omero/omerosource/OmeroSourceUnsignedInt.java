package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.ij2command.OmeroTools;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.integer.UnsignedIntType;
import net.imglib2.view.Views;
import omero.api.RawPixelsStorePrx;

import java.util.concurrent.ConcurrentHashMap;

public class OmeroSourceUnsignedInt extends OmeroSource<UnsignedIntType> {
    public OmeroSourceUnsignedInt(OmeroSourceOpener opener, int c) throws Exception {
        super(opener, c);
    }

    @Override
    public RandomAccessibleInterval<UnsignedIntType> createSource(int t, int level) {

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
            final Img<UnsignedIntType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedIntType(),
                    cell -> {

                        try {
                            synchronized (OmeroTools.class) {
                                RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);
                                rawPixStore.setPixelsId(this.opener.getPixelsID(), false);

                                //setResolutionLevels indexes are in reverse order compared to the other methods
                                //here index 0 is the lowest resolution and n-1 is the highest
                                rawPixStore.setResolutionLevel(this.opener.getNLevels()-1-level);

                                Cursor<UnsignedIntType> out = Views.flatIterable(cell).cursor();

                                //cell connait sa position dans l'espace (dans la grande image)
                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, sx);

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, sy);

                                int w = maxX - minX;
                                int h = maxY - minY;

                                byte[] bytes = rawPixStore.getTile((int) cell.min(2), channel_index, t, minX, minY, w, h);

                                int totBytes = (w * h) * 4;
                                int idxPx = 0;

                                boolean littleEndian = false;
                                if (littleEndian) { // TODO improve this dirty switch block
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ( (bytes[idxPx + 3] & 0xff) << 24) | ((bytes[idxPx + 2] & 0xff) << 16) | ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx] & 0xff);
                                        out.next().set(v);
                                        idxPx += 4;
                                    }
                                } else {
                                    while ((out.hasNext()) && (idxPx < totBytes)) {
                                        int v = ( (bytes[idxPx] & 0xff) << 24) | ((bytes[idxPx + 1] & 0xff) << 16) | ((bytes[idxPx + 2] & 0xff) << 8) | (bytes[idxPx + 3] & 0xff);
                                        out.next().set(v);
                                        idxPx += 4;
                                    }
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
    public UnsignedIntType getType() {
        return new UnsignedIntType();
    }

}
