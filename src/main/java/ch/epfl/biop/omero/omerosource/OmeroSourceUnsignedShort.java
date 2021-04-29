package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.ij2command.OmeroTools;
import loci.formats.IFormatReader;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.view.Views;
import omero.api.RawPixelsStorePrx;

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
            final Img<UnsignedShortType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedShortType(),
                cell -> {
                    try {
                        RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);
                        // chopper l'ID pixel dans le opener
                        rawPixStore.setPixelsId(this.opener.getPixelsID(), false);
                        //RandomAccessibleInterval<UnsignedShortType> rai = OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,level,ctx,gt);
                        rawPixStore.setResolutionLevel(level);

                        Cursor<UnsignedShortType> out = Views.flatIterable(cell).cursor();

                        int minX = (int) cell.min(0);
                        int maxX = Math.min(minX + xc, sx);

                        int minY = (int) cell.min(1);
                        int maxY = Math.min(minY + yc, sy);

                        int w = maxX - minX;
                        int h = maxY - minY;

                        int totBytes = (w * h) * 2;
                        int idxPx = 0;

                        //byte[] bytes = reader.openBytes(switchZandC ? reader.getIndex(cChannel, z, t) : reader.getIndex(z, cChannel, t), minX, minY, w, h);
                        //TODO change z!
                        byte[] bytes = rawPixStore.getTile(0, channel_index, t, minX, minY, w, h);

                        boolean littleEndian = false;
                        if (littleEndian) { // TODO improve this dirty switch block
                            while ((out.hasNext()) && (idxPx < totBytes)) {
                                int v = ((bytes[idxPx + 1] & 0xff) << 8) | (bytes[idxPx] & 0xff);
                                out.next().set(v);
                                idxPx += 2;
                            }
                        } else {
                            while ((out.hasNext()) && (idxPx < totBytes)) {
                                int v = ((bytes[idxPx] & 0xff) << 8) | (bytes[idxPx + 1] & 0xff);
                                out.next().set(v);
                                idxPx += 2;
                            }
                        }
                        rawPixStore.close();
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
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }

}
