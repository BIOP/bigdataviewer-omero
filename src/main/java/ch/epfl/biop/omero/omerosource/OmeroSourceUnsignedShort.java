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
        /*
        try {
            if (!raiMap.containsKey(t)) {
                raiMap.put(t, new ConcurrentHashMap<>());
            }

            //RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);
            // img.getDefaultPixels() == pixels (PixelsData)
            //rawPixStore.setPixelsId(img.getDefaultPixels().getId(), false);
            //RandomAccessibleInterval<UnsignedShortType> rai = OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,level,ctx,gt);

            // Create cached image factory of Type Byte
            ReadOnlyCachedCellImgOptions options = new ReadOnlyCachedCellImgOptions();
            // Put cell dimensions to arbitrary values
            //TODO : change this to rawPixStore.getTileSize()
            final int Xdefaultcellsize = 512;
            final int Ydefaultcellsize = 512;


            options = options.cellDimensions(Xdefaultcellsize,Ydefaultcellsize, 1);
            final ReadOnlyCachedCellImgFactory factory = new ReadOnlyCachedCellImgFactory(options);

            // Creates image, with cell Consumer method, which creates the image
            final Img<UnsignedShortType> rai = factory.create(new long[]{sx, sy, sz}, new UnsignedShortType(),
                    cell -> {
                        try {
                            IFormatReader reader = readerPool.acquire();
                            reader.setSeries(this.cSerie);
                            reader.setResolution(level);
                            Cursor<UnsignedShortType> out = Views.flatIterable(cell).cursor();
                            int minZ = (int) cell.min(2);
                            int maxZ = Math.min(minZ + zc, reader.getSizeZ());

                            for (int z = minZ; z < maxZ; z++) {
                                int minX = (int) cell.min(0);
                                int maxX = Math.min(minX + xc, reader.getSizeX());

                                int minY = (int) cell.min(1);
                                int maxY = Math.min(minY + yc, reader.getSizeY());

                                int w = maxX - minX;
                                int h = maxY - minY;


                                int totBytes = (w * h) * 2;

                                int idxPx = 0;

                                byte[] bytes = reader.openBytes(switchZandC ? reader.getIndex(cChannel, z, t) : reader.getIndex(z, cChannel, t), minX, minY, w, h);

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
                            }
                            readerPool.recycle(reader);
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
        */

        return null;
    }

    @Override
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }

}
