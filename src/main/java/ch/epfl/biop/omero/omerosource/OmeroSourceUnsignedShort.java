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
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.ImageData;

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
                        synchronized (OmeroTools.class) {
                            RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);

                            //ImageData img = gt.getFacility(BrowseFacility.class).getImage(ctx, imageID);
                            //RawPixelsStorePrx rawPixStore = gt.getPixelsStore(ctx);
                            //rawPixStore.setPixelsId(img.getDefaultPixels().getId(), false);
                            System.out.println("loader image ID : " +imageID);
                            System.out.println("loader pixel ID : " +this.opener.getPixelsID());
                            System.out.println(ctx);
                            System.out.println(gt);
                            rawPixStore.setPixelsId(this.opener.getPixelsID(), false);
                            //RandomAccessibleInterval<UnsignedShortType> rai = OmeroTools.openTiledRawRandomAccessibleInterval(imageID,channel_index,t,level,ctx,gt);
                            //test different resolution levels
                            //TODO: change this
                            rawPixStore.setResolutionLevel(5-level);

                            System.out.println("loader current level : "+rawPixStore.getResolutionLevel());

                            Cursor<UnsignedShortType> out = Views.flatIterable(cell).cursor();

                            //cell connait sa position dans l'espace (dans la grande image)
                            int minX = (int) cell.min(0);
                            System.out.println("sx "+sx);
                            System.out.println("minX "+minX);
                            //System.out.println("size true X : " + rawPixStore.getResolutionDescriptions()[level].sizeX);
                            int maxX = Math.min(minX + xc, sx);
                            System.out.println("maxX "+maxX);

                            int minY = (int) cell.min(1);
                            int maxY = Math.min(minY + yc, sy);
                            System.out.println("minY "+minY);
                            System.out.println("maxY "+maxY);

                            int w = maxX - minX;
                            int h = maxY - minY;

                            int totBytes = (w * h) * 2;
                            int idxPx = 0;

                            System.out.println("X "+ minX);
                            System.out.println("Y "+ minY);

                            //byte[] bytes = reader.openBytes(switchZandC ? reader.getIndex(cChannel, z, t) : reader.getIndex(z, cChannel, t), minX, minY, w, h);
                            //byte[] bytes = rawPixStore.getTile((int) cell.min(2), channel_index, t, minX, minY, w, h);
                            //System.out.println("level : " + level + ": "+ minX + " "+ minY);
                            byte[] bytes = rawPixStore.getTile(0, 0, 0, minX, minY, w, h);
                            //byte[] bytes = rawPixStore.getTile(0, 0, 0, 0, 0, w, h);
                            //byte[] bytes = rawPixStore.getTile(0, 0, 0, 50, 50, w, h);
                            //System.out.println("level : " + level + ": "+ minX + " "+ minY + " Success");
                            //byte[] bytes = new byte[w*h*2];


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
    public UnsignedShortType getType() {
        return new UnsignedShortType();
    }

}