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
package ch.epfl.biop.omero.omerosource;

import ch.epfl.biop.ij2command.OmeroTools;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.ReadOnlyCachedCellImgFactory;
import net.imglib2.cache.img.ReadOnlyCachedCellImgOptions;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import omero.api.RawPixelsStorePrx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

public class OmeroSourceFloat extends OmeroSource<FloatType> {
    public OmeroSourceFloat(OmeroSourceOpener opener, int c) throws Exception {
        super(opener, c);
    }

    @Override
    public RandomAccessibleInterval<FloatType> createSource(int t, int level) {

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
            final Img<FloatType> rai = factory.create(new long[]{sx, sy, sz}, new FloatType(),
                    cell -> {
                        // get a rawPixelsStore from the rawPixelsStorePool to avoid creating a new instance of rawPixelsStore in each thread.
                        RawPixelsStorePrx rawPixStore = opener.pool.acquire();

                        //setResolutionLevels indexes are in reverse order compared to the other methods
                        //here index 0 is the lowest resolution and n-1 is the highest
                        rawPixStore.setResolutionLevel(this.opener.getNLevels()-1-level);

                        Cursor<FloatType> out = Views.flatIterable(cell).cursor();

                        //cell connait sa position dans l'espace (dans la grande image)
                        int minX = (int) cell.min(0);
                        int maxX = Math.min(minX + xc, sx);

                        int minY = (int) cell.min(1);
                        int maxY = Math.min(minY + yc, sy);

                        int w = maxX - minX;
                        int h = maxY - minY;

                        byte[] bytes = rawPixStore.getTile((int) cell.min(2), channel_index, t, minX, minY, w, h);

                        int totBytes = (w * h)*4;
                        int idxPx = 0;

                        // TODO change this boolean value?
                        boolean littleEndian = false;

                        byte[] curBytes = new byte[4];
                        if (littleEndian) { // TODO improve this dirty switch block
                            while ((out.hasNext()) && (idxPx < totBytes)) {
                                curBytes[0]= bytes[idxPx];
                                curBytes[1]= bytes[idxPx+1];
                                curBytes[2]= bytes[idxPx+2];
                                curBytes[3]= bytes[idxPx+3];
                                out.next().set( ByteBuffer.wrap(curBytes).order(ByteOrder.LITTLE_ENDIAN).getFloat());
                                idxPx += 4;
                            }
                        } else {
                            while ((out.hasNext()) && (idxPx < totBytes)) {
                                curBytes[0]= bytes[idxPx];
                                curBytes[1]= bytes[idxPx+1];
                                curBytes[2]= bytes[idxPx+2];
                                curBytes[3]= bytes[idxPx+3];
                                out.next().set( ByteBuffer.wrap(curBytes).order(ByteOrder.BIG_ENDIAN).getFloat());
                                idxPx += 4;
                            }
                        }
                        //recycle the rawPixelsStore so that it can be used by another thread.
                        opener.pool.recycle(rawPixStore);
                    });

            raiMap.get(t).put(level, rai);
            return raiMap.get(t).get(level);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public FloatType getType() {
        return new FloatType();
    }

}
