/****************************************************************************
Copyright (c) 2014, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is 
available at http://www.eclipse.org/legal/cpl-v10.html
****************************************************************************/
package mef;

import java.util.*;
import edu.mines.jtk.awt.*;
import edu.mines.jtk.dsp.*;
import edu.mines.jtk.util.*;

import static edu.mines.jtk.util.Parallel.*;
import static edu.mines.jtk.util.ArrayMath.*;
import static mef.FaultGeometry.*;

import util.*;

/**
 * Computes fault skins from images of fault likelihoods, strikes and dips. A
 * fault skin is a linked list of fault cells. Each fault cell is an oriented
 * point located on a ridge in an image of fault likelihood. Each image sample
 * corresponds to either no cell or one cell.
 * <p>
 * A cell has up to four neighbors ("nabors") that lie above, below, left and
 * right of the cell when viewed from above the fault, that is, when looking
 * from the hanging wall toward the footwall. Links to nabors enables cells to
 * form a skin of connected cells, which represents a fault.
 * <p>
 * Links to left and right cell nabors can be used to iterate over all cells
 * along a fault trace, a path of constant depth that is everywhere tangent to
 * fault strike. Likewise, links to cell nabors above and below a cell can be
 * used to iterate up or down a fault. However, this simple up or down
 * iteration typically does not coincide with a fault curve that is everywhere
 * tangent to fault dip.
 * <p>
 * A fault skin is grown by linking nearby cells having similar properties,
 * beginning with a seed cell that has sufficiently high fault likelihood.
 * Several methods in this class set parameters that control this growing
 * process.
 *
 * @author Dave Hale, Colorado School of Mines
 * @version 2014.07.03
 */
public class FaultSkinner {

  /**
   * Constructs a fault skinner with default parameters.
   */
  public FaultSkinner() {
    _fs1min = -Float.MAX_VALUE;
    _fs1max =  Float.MAX_VALUE;
    _fllo = 0.2f;
    _flhi = 0.8f;
    _dflmax = 0.2f;
    _dfpmax = 30.0f;
    _dftmax = 10.0f;
    _dnpmax = 0.5f;
    _ds1max = 1.0f;
    _ncsmin = 400;
  }

  /**
   * Sets the minimum number of cells in a skin. Skins smaller than this will
   * be discarded.
   * <p>
   * The default minimum skin size is 400.
   */
  public void setMinSkinSize(int minSize) {
    _ncsmin = minSize;
  }

  /**
   * Sets lower and upper bounds on fault throw for cells in a skin.
   * These bounds should be set only after fault dip slips have been 
   * computed for all cells used to grow skins.
   * <p>
   * The default bounds are huge, so that throws are unrestricted.
   * @param minThrow the lower bound.
   * @param maxThrow the upper bound.
   */
  public void setMinMaxThrow(double minThrow, double maxThrow) {
    Check.argument(minThrow<=maxThrow,"minThrow does not exceed maxThrow");
    _fs1min = (float)minThrow;
    _fs1max = (float)maxThrow;
  }

  /**
   * Sets fault likelihood thresholds used to grow skins. Cells in a skin
   * should have, or be connected to cells that have, high fault likelihoods.
   * All cells in a skin will have fault likelihoods not less than the lower
   * threshold. At least one cell in a skin will have a fault likelihood not
   * less than the upper threshold. 
   * <p>
   * The default thresholds are 0.2 and 0.8, respectively.
   * @param lowerLikelihood lower threshold for fault likelihood.
   * @param upperLikelihood upper threshold for fault likelihood.
   */
  public void setGrowLikelihoods(
      double lowerLikelihood, double upperLikelihood) {
    Check.argument(lowerLikelihood<=upperLikelihood,
        "lowerLikelihood does not exceed upperLikelihood");
    _fllo = (float)lowerLikelihood;
    _flhi = (float)upperLikelihood;
  }

  /**
   * Sets the maximum difference in fault likelihood for a cell and its nabors.
   * <p>
   * The default maximum difference is 0.2.
   * @param maxDeltaLikelihood upper bound on difference in fault likelihood.
   */
  public void setMaxDeltaLikelihood(double maxDeltaLikelihood) {
    _dflmax = (float)maxDeltaLikelihood;
  }

  /**
   * Sets the maximum difference in fault strike for a cell and its nabors.
   * @param maxDeltaStrike upper bound on difference in fault strike.
   * <p>
   * The default maximum difference is 30 degrees.
   */
  public void setMaxDeltaStrike(double maxDeltaStrike) {
    _dfpmax = (float)maxDeltaStrike;
  }

  /**
   * Sets the maximum difference in fault dip for a cell and its nabors.
   * @param maxDeltaDip upper bound on difference in fault dip.
   * <p>
   * The default maximum difference is 10 degrees.
   */
  public void setMaxDeltaDip(double maxDeltaDip) {
    _dftmax = (float)maxDeltaDip;
  }

  /**
   * Sets the maximum difference in fault throw for a cell and its nabors.
   * @param maxDeltaThrow upper bound on difference in fault throw.
   * <p>
   * The default maximum difference is 1.0 samples.
   */
  public void setMaxDeltaThrow(double maxDeltaThrow) {
    _ds1max = (float)maxDeltaThrow;
  }

  /**
   * Sets the threshold planar distance. A cell should lie near the planes of
   * nabor cells. The specified threshold is the maximum distance to nabor
   * planes.
   * <p>
   * The default maximim planar distance is 0.5 samples.
   * @param maxPlanarDistance upper bound on planar distance, in samples.
   */
  public void setMaxPlanarDistance(double maxPlanarDistance) {
    _dnpmax = (float)maxPlanarDistance;
  }

  /**
   * Returns array of cells in ridge surfaces of fault likelihood.
   * @param flpt array {fl,fp,ft} of fault likelihoods, strikes and dips.
   * @return array of cells.
   */
  public FaultCell[] findCells(float[][][][] flpt) {
    return cells(flpt);
  }

  /**
   * Returns an array of skins comprised of specified cells. Some cells may be
   * unused. For example, cells with fault likelihoods less than the lower
   * threshold for growing skins will be unused. Likewise, cells that do not
   * form a skin with sufficient size will be unused.
   * @param cells array of cells from which to grow skins.
   * @return array of skins.
   */
  public FaultSkin[] findSkins(int n1, int n2, int n3, FaultCell[] cells) {
    return skins(n1,n2,n3,cells);
  }

  /**
   * Gets arrays {xyz,uvw,rgb} of cell coordinates, normals and colors.
   * In these arrays, cells are represented by quads with specified size.
   * @param size the size (in samples) of the quads.
   * @param cells the cells for which to compute quads.
   */
  public static float[][] getXyzUvwRgb(float size, FaultCell[] cells) {
    FloatList xyz = new FloatList();
    FloatList uvw = new FloatList();
    FloatList fcl = new FloatList();
    size *= 0.5f;
    float[] qa = {0.0f,-size,-size};
    float[] qb = {0.0f, size,-size};
    float[] qc = {0.0f, size, size};
    float[] qd = {0.0f,-size, size};
    for (FaultCell cell:cells) {
      float x1 = cell.x1;
      float x2 = cell.x2;
      float x3 = cell.x3;
      float w1 = cell.w1;
      float w2 = cell.w2;
      float w3 = cell.w3;
      float fl = cell.fl;
      float fp = toRadians(cell.fp);
      float ft = toRadians(cell.ft);
      float cp = cos(fp);
      float sp = sin(fp);
      float ct = cos(ft);
      float st = sin(ft);
      float[] ra = rotatePoint(cp,sp,ct,st,qa);
      float[] rb = rotatePoint(cp,sp,ct,st,qb);
      float[] rc = rotatePoint(cp,sp,ct,st,qc);
      float[] rd = rotatePoint(cp,sp,ct,st,qd);
      float a1 = x1+ra[0], a2 = x2+ra[1], a3 = x3+ra[2];
      float b1 = x1+rb[0], b2 = x2+rb[1], b3 = x3+rb[2];
      float c1 = x1+rc[0], c2 = x2+rc[1], c3 = x3+rc[2];
      float d1 = x1+rd[0], d2 = x2+rd[1], d3 = x3+rd[2];
      xyz.add(a3); xyz.add(a2); xyz.add(a1); fcl.add(fl);
      xyz.add(b3); xyz.add(b2); xyz.add(b1); fcl.add(fl);
      xyz.add(c3); xyz.add(c2); xyz.add(c1); fcl.add(fl);
      xyz.add(d3); xyz.add(d2); xyz.add(d1); fcl.add(fl);
      uvw.add(w3); uvw.add(w2); uvw.add(w1);
      uvw.add(w3); uvw.add(w2); uvw.add(w1);
      uvw.add(w3); uvw.add(w2); uvw.add(w1);
      uvw.add(w3); uvw.add(w2); uvw.add(w1);
    }
    float[] fc = fcl.trim();
    float fcmin = 0.0f;
    float fcmax = 1.0f;
    ColorMap cmap = new ColorMap(fcmin,fcmax,ColorMap.JET);
    float[] rgb = cmap.getRgbFloats(fc);
    return new float[][]{xyz.trim(),uvw.trim(),rgb};
  }

  ///////////////////////////////////////////////////////////////////////////
  // private

  private float _fllo; // lower threshold on fault likelihoods
  private float _flhi; // higher threshold on fault likelihoods
  private float _fs1min; // min fault throw
  private float _fs1max; // max fault throw
  private float _dflmax; // max difference between likelihoods of nabors
  private float _dfpmax; // max difference between strikes of nabors
  private float _dftmax; // max difference between dips of nabors
  private float _ds1max; // max difference between throws of nabors
  private float _dnpmax; // max distance to planes of nabors
  private int _ncsmin; // min number of cells that form a skin
  private FaultCell[][][] _cells; // array of all fault cells
  private int _w1=10,_w2=10,_w3=10;
  private float[][][] _gw = gaussWeights();

  // Uses fault images to find cells, oriented points located on ridges.
  private FaultCell[] cells(float[][][][] flpt) {
    float[][][] f = flpt[0];
    float[][][] p = flpt[1];
    float[][][] t = flpt[2];
    int n1 = f[0][0].length;
    int n2 = f[0].length;
    int n3 = f.length;

    // Smooth fault likelihoods in 2nd and 3rd dimensions. This helps to
    // eliminate spurious ridges, and improves the accuracy of 2nd-order
    // finite-difference approximations (parabolic interpolation) used to
    // locate ridges.
    float[][][] fs = new float[n3][n2][n1];
    RecursiveGaussianFilterP rgf = new RecursiveGaussianFilterP(1.0);
    rgf.applyX0X(f,fs);
    rgf.applyXX0(fs,fs);
    f = fs;

    // Vertical image boundaries are discontinuities that may look like
    // faults. If a fault appears to be near and nearly parallel to image
    // boundaries, then assume it is a boundary artifact and not truly a
    // fault.
    int imax = 5; // max number of samples considered to be near boundary
    float wwmax = 0.75f; // cosine of 30 degrees, squared

    // Loop over all samples. Construct cells for samples nearest to ridges.
    ArrayList<FaultCell> cellList = new ArrayList<FaultCell>();
    for (int i3=0; i3<n3; ++i3) {
      int i3m = max(i3-1,0);
      int i3p = min(i3+1,n3-1);
      for (int i2=0; i2<n2; ++i2) {
        int i2m = max(i2-1,0);
        int i2p = min(i2+1,n2-1);
        float[] fmi = f[i3m][i2 ];
        float[] fim = f[i3 ][i2m];
        float[] fip = f[i3 ][i2p];
        float[] fpi = f[i3p][i2 ];
        float[] fmm = f[i3m][i2m];
        float[] fpp = f[i3p][i2p];
        float[] fmp = f[i3m][i2p];
        float[] fpm = f[i3p][i2m];
        float[] fii = f[i3 ][i2 ];
        float[] pii = p[i3 ][i2 ];
        float[] tii = t[i3 ][i2 ];
        for (int i1=0; i1<n1; ++i1) {
          float fmii = fmi[i1 ];
          float fimi = fim[i1 ];
          float fipi = fip[i1 ];
          float fpii = fpi[i1 ];
          float fmmi = fmm[i1 ];
          float fppi = fpp[i1 ];
          float fmpi = fmp[i1 ];
          float fpmi = fpm[i1 ];
          float fiii = fii[i1 ];
          float piii = pii[i1 ];
          float tiii = tii[i1 ];

          // Most image samples will not have a fault cell.
          FaultCell cell = null;

          // Accumulators for ridge likelihoods and locations. Depending on
          // the limits on fault strike used below, we may find more than one
          // ridge.
          float nr = 0;
          float fl = 0.0f;
          float d2 = 0.0f;
          float d3 = 0.0f;

          // If S-N ridge, ...
          if ((fipi<fiii && fimi<fiii) &&
              ((337.5f<=piii || piii<= 22.5f) || 
               (157.5f<=piii && piii<=202.5f))) {
            float f1 = 0.5f*(fipi-fimi); // 1st derivative
            float f2 = fipi-2.0f*fiii+fimi; // 2nd derivative
            float dr = -f1/f2; // signed distance to ridge
            float fr = fiii+f1*dr+0.5f*f2*dr*dr; // fault likelihood
            if (fr>=_fllo) {
              float[] w = faultNormalVectorFromStrikeAndDip(piii,tiii);
              float w2 = w[1];
              if (imax<=i2 && i2<n2-imax || w2*w2<=wwmax) {
                fl += fr;
                d2 += dr;
                nr += 1;
              }
            }
          }

          // If SW-NE ridge, ...
          if ((fmpi<fiii && fpmi<fiii) &&
              (( 22.5f<=piii && piii<= 67.5f) || 
               (202.5f<=piii && piii<=247.5f))) {
            float f1 = 0.5f*(fmpi-fpmi); // 1st derivative
            float f2 = fmpi-2.0f*fiii+fpmi; // 2nd derivative
            float dr = -f1/f2; // signed distance to ridge
            float fr = fiii+f1*dr+0.5f*f2*dr*dr; // fault likelihood
            if (fr>=_fllo) {
              float[] w = faultNormalVectorFromStrikeAndDip(piii,tiii);
              float w2 = w[1], w3 = w[2];
              if ((imax<=i2 && i2<n2-imax || w2*w2<=wwmax) &&
                  (imax<=i3 && i3<n3-imax || w3*w3<=wwmax)) {
                fl += fr;
                d2 += dr;
                d3 -= dr;
                nr += 1;
              }
            }
          }

          // If W-E ridge, ...
          if ((fpii<fiii && fmii<fiii) &&
              (( 67.5f<=piii && piii<=112.5f) ||
               (247.5f<=piii && piii<=292.5f))) {
            float f1 = 0.5f*(fpii-fmii); // 1st derivative
            float f2 = fmii-2.0f*fiii+fpii; // 2nd derivative
            float dr = -f1/f2; // signed distance to ridge
            float fr = fiii+f1*dr+0.5f*f2*dr*dr; // fault likelihood
            if (fr>=_fllo) {
              float[] w = faultNormalVectorFromStrikeAndDip(piii,tiii);
              float w3 = w[2];
              if (imax<=i3 && i3<n3-imax || w3*w3<=wwmax) {
                fl += fr;
                d3 += dr;
                nr += 1;
              }
            }
          }

          // If NW-SE ridge, ...
          if ((fppi<fiii && fmmi<fiii) &&
              ((112.5f<=piii && piii<=157.5f) || 
               (292.5f<=piii && piii<=337.5f))) {
            float f1 = 0.5f*(fppi-fmmi); // 1st derivative
            float f2 = fppi-2.0f*fiii+fmmi; // 2nd derivative
            float dr = -f1/f2; // signed distance to ridge
            float fr = fiii+f1*dr+0.5f*f2*dr*dr; // fault likelihood
            if (fr>=_fllo) {
              float[] w = faultNormalVectorFromStrikeAndDip(piii,tiii);
              float w2 = w[1], w3 = w[2];
              if ((imax<=i2 && i2<n2-imax || w2*w2<=wwmax) &&
                  (imax<=i3 && i3<n3-imax || w3*w3<=wwmax)) {
                fl += fr;
                d2 += dr;
                d3 += dr;
                nr += 1;
              }
            }
          }

          // If at least one ridge, construct a cell and add to list.
          if (nr>0) {
            fl /= nr;
            d2 /= nr;
            d3 /= nr;
            cell = new FaultCell(i1,i2+d2,i3+d3,fl,piii,tiii);
            cellList.add(cell);
          }
        }
      }
    }
    return cellList.toArray(new FaultCell[0]);
  }

  // Returns skins constructed from specified cells.
  private FaultSkin[] skins(int n1, int n2, int n3, FaultCell[] cells) {
    int ncell = cells.length;
    _cells = new FaultCell[n3][n3][n1];
    for (FaultCell cell:cells) {
      cell.skin=null;
      int i1 = cell.i1; if(i1>n1-1) i1=n1-1;
      int i2 = cell.i2; if(i2>n2-1) i2=n2-1;
      int i3 = cell.i3; if(i3>n3-1) i3=n3-1;
      _cells[i3][i2][i1] = cell;
    }

    // Empty list of skins.
    ArrayList<FaultSkin> skinList = new ArrayList<FaultSkin>();

    // Cell comparator for high-to-low ordering based on fault likelihoods.
    Comparator<FaultCell> flComparator = new Comparator<FaultCell>() {
      public int compare(FaultCell c1, FaultCell c2) {
        if (c1.fl<c2.fl)
          return 1;
        else if (c1.fl>c2.fl)
          return -1;
        else
          return 0;
      }
    };

    // Make a list of cells that might be seeds for new skins.
    ArrayList<FaultCell> seedList = new ArrayList<FaultCell>();
    for (int icell=0; icell<ncell; ++icell) {
      FaultCell cell = cells[icell];
      if (cell.fl>=_flhi && cell.s1>=_fs1min && cell.s1<=_fs1max)
        seedList.add(cell);
    }
    int nseed = seedList.size();

    // Sort the list of seeds high-to-low by fault likelihood.
    FaultCell[] seeds = seedList.toArray(new FaultCell[0]);
    Arrays.sort(seeds,flComparator);
    seedList.clear();
    for (FaultCell seed:seeds)
      seedList.add(seed);

    // While potential seeds remain, ...
    for (int kseed=0; kseed<nseed; ++kseed) {

      // Skip any potential seeds that are already in a skin.
      while (kseed<nseed && seedList.get(kseed).skin!=null)
        ++kseed;

      // If we found a seed with which to construct a new skin, ...
      if (kseed<nseed) {
        FaultCell seed = seedList.get(kseed);

        // Make a new empty skin.
        FaultSkin skin = new FaultSkin();

        // Make a priority queue of cells, initially with only the seed.
        PriorityQueue<FaultCell> growQueue = 
            new PriorityQueue<FaultCell>(1024,flComparator);
        growQueue.add(seed);

        // While the grow queue is not empty, ...
        while (!growQueue.isEmpty()) {

          // Get and remove the cell with highest fault likelihood from the
          // grow queue. If not already in the skin, add them and link and
          // add any mutually best nabors to the grow queue.
          FaultCell cell = growQueue.poll();
          if (cell.skin==null) {
            FaultCell[] nbs = findNabors(n1,n2,n3,cell);
            if (nbs!=null) {
              FaultCell cc = nbs[0];
              FaultCell ca = nbs[1];
              FaultCell cb = nbs[2];
              FaultCell cl = nbs[3];
              FaultCell cr = nbs[4];
              skin.add(cc);
              if (ca!=null) {
                linkAboveBelow(ca,cc);
                growQueue.add(ca);
              }
              if (cb!=null) {
                linkAboveBelow(cc,cb);
                growQueue.add(cb);
              }
              if (cl!=null) {
                linkLeftRight(cl,cc);
                growQueue.add(cl);
              }
              if (cr!=null) {
                linkLeftRight(cc,cr);
                growQueue.add(cr);
              }
            }
          }
        }
        // Done growing. Add this skin to the list of skins. Here we include
        // skins that are too small. If we did not include them here, we would
        // need to put them in a list of small skins, so that we could later
        // remove all of their cells. (By not removing those cells now, we
        // prevent them from becoming parts of other skins.) Instead, we
        // simply put all skins in the list, and filter that list later.
        trace("skin="+skin.size());
        skinList.add(skin);
        updateCells(n1,n2,n3,skin);
      }
    }

    // Filter skins to include only those that are big enough. Remove all
    // cells from any skins that are too small.
    ArrayList<FaultSkin> bigSkinList = new ArrayList<FaultSkin>();
    for (FaultSkin skin:skinList) {
      if (skin.size()>=_ncsmin) {
        bigSkinList.add(skin);
      } else {
        for (FaultCell cell:skin) {
          cell.skin = null;
          cell.ca = null;
          cell.cb = null;
          cell.cl = null;
          cell.cr = null;
        }
      }
    }
    return bigSkinList.toArray(new FaultSkin[0]);
  }

  private void updateCells(int n1, int n2, int n3, FaultSkin fs) {
    int d = 3;
    FaultCell[] fcs = FaultSkin.getCells(new FaultSkin[]{fs});
    int nc = fcs.length;
    for (int ic=0; ic<nc; ++ic) {
      FaultCell fc = fcs[ic];
      int i1 = fc.i1;
      int i2 = fc.i2;
      int i3 = fc.i3;
      int b1 = max(i1-d,0), e1 = min(i1+d,n1-1);
      int b2 = max(i2-d,0), e2 = min(i2+d,n2-1);
      int b3 = max(i3-d,0), e3 = min(i3+d,n3-1);
      for (int k3=b3; k3<=e3; k3++) {
      for (int k2=b2; k2<=e2; k2++) {
      for (int k1=b1; k1<=e1; k1++) {
        FaultCell fcx = _cells[k3][k2][k1];
        if (fcx!=null) {
          float dp = fc.fp-fcx.fp;
          dp = min(abs(dp),abs(dp+360f),abs(dp-360f));
          if(dp<10f){fcx.skin=fs;}
        }
      }}}
    }
  }


  private FaultCell[] findNabors(int n1, int n2, int n3, FaultCell cell) {
    float fp = cell.fp;
    float ft = cell.ft;
    final int i1 = cell.i1;
    final int i2 = cell.i2;
    final int i3 = cell.i3;
    int b1 = max(i1-_w1,0), e1 = min(i1+_w1,n1-1);
    int b2 = max(i2-_w2,0), e2 = min(i2+_w2,n2-1);
    int b3 = max(i3-_w3,0), e3 = min(i3+_w3,n3-1);
    final FaultCell[] fcs = findCellsInBox(b1,e1,b2,e2,b3,e3,fp,ft);
    final int nc = fcs.length;
    if (nc<5) {return null;}
    final int m1=11, m2=11, m3=11;
    final float[][][][] flpt = new float[3][m3][m2][m1];
    loop(m3,new Parallel.LoopInt(){
    public void compute(int k3) {
      int p3 = k3+i3-5;
      float[][] fl3 = flpt[0][k3];
      float[][] fp3 = flpt[1][k3];
      float[][] ft3 = flpt[2][k3];
      for (int k2=0; k2<m2; ++k2) {
        int p2 = k2+i2-5;
      for (int k1=0; k1<m1; ++k1) {
        int p1 = k1+i1-5;
        float g11 = 0f;
        float g12 = 0f;
        float g13 = 0f;
        float g22 = 0f;
        float g23 = 0f;
        float g33 = 0f;
        for (FaultCell fc:fcs) {
          float w1 = fc.w1;
          float w2 = fc.w2;
          float w3 = fc.w3;
          int r1 = p1-fc.i1;
          int r2 = p2-fc.i2;
          int r3 = p3-fc.i3;
          float rs = sqrt(r1*r1+r2*r2+r3*r3);
          float gw = _gw[abs(r3)][abs(r2)][abs(r1)]*fc.fl;
          if(rs>0f) {
            r1 /= rs; r2 /= rs; r3 /= rs;
            float wr = w1*r1+w2*r2+w3*r3;
            wr = 1f-wr*wr;
            wr *= wr;
            wr *= wr;
            gw *= wr;
          }
          g11 += w1*w1*gw;
          g12 += w1*w2*gw;
          g13 += w1*w3*gw;
          g22 += w2*w2*gw;
          g23 += w2*w3*gw;
          g33 += w3*w3*gw;
        }
        double[][] a = new double[3][3];
        a[0][0] = g11; a[0][1] = g12; a[0][2] = g13;
        a[1][0] = g12; a[1][1] = g22; a[1][2] = g23;
        a[2][0] = g13; a[2][1] = g23; a[2][2] = g33;
        float[][] ue = solveEigenproblems(a);
        float eui = ue[1][0];
        float evi = ue[1][1];
        float u1i = ue[0][0];
        float u2i = ue[0][1];
        float u3i = ue[0][2];
        if (u2i==0.0f&&u3i==0f){continue;}
        if (u1i>0.0f) {
          u1i = -u1i;
          u2i = -u2i;
          u3i = -u3i;
        }
        fl3[k2][k1] = (eui-evi);
        ft3[k2][k1] = faultDipFromNormalVector(u1i,u2i,u3i);
        fp3[k2][k1] = faultStrikeFromNormalVector(u1i,u2i,u3i);
      }}
    }});
    sub(flpt[0],min(flpt[0]),flpt[0]);
    div(flpt[0],max(flpt[0]),flpt[0]);
    FaultCell[] cells = cells(flpt);
    if (cells==null) {return null;}
    FaultCell[] nbs = new FaultCell[5];
    FaultCell cc = centerCell(cells);
    if (cc==null) {return null;}
    nbs[0] = new FaultCell(cc.x1+i1-5,cc.x2+i2-5,cc.x3+i3-5,cc.fl,cc.fp,cc.ft);
    FaultCellGrid fcg = new FaultCellGrid(cells);
    FaultCell ca,cb,cl,cr;
    // look for the above nabor
    if (cell.ca==null) {
      ca = findNaborAbove(fcg,cc);
      cb = findNaborBelow(fcg,ca);
      if (ca!=null && ca.skin==null && cb==cc) {
        nbs[1] = new FaultCell(ca.x1+i1-5,ca.x2+i2-5,ca.x3+i3-5,ca.fl,ca.fp,ca.ft);
      }
    }
    // look for the below nabor
    if (cell.cb==null) {
      cb = findNaborBelow(fcg,cc);
      ca = findNaborAbove(fcg,cb);
      if (cb!=null && cb.skin==null && ca==cc) {
        nbs[2] = new FaultCell(cb.x1+i1-5,cb.x2+i2-5,cb.x3+i3-5,cb.fl,cb.fp,cb.ft);
      }
    }
    // look for the left nabor
    if (cell.cl==null) {
      cl = findNaborLeft(fcg,cc);
      cr = findNaborRight(fcg,cl);
      if (cl!=null && cl.skin==null && cr==cc) {
        nbs[3] = new FaultCell(cl.x1+i1-5,cl.x2+i2-5,cl.x3+i3-5,cl.fl,cl.fp,cl.ft);
      }
    }
    // look for the right nabor
    if (cell.cr==null) {
      cr = findNaborRight(fcg,cc);
      cl = findNaborLeft(fcg,cr);
      if (cr!=null && cr.skin==null && cl==cc) {
       nbs[4] = new FaultCell(cr.x1+i1-5,cr.x2+i2-5,cr.x3+i3-5,cr.fl,cr.fp,cr.ft);
      }
    }
    return nbs;
  }

  private FaultCell centerCell(FaultCell[] fcs) {
    FaultCell cell = null;
    float ds = Float.MAX_VALUE;
    for (FaultCell fci:fcs) {
      if(fci.i1!=5){continue;}
      float d1 = 5-fci.x1;
      float d2 = 5-fci.x2;
      float d3 = 5-fci.x3;
      float di = d1*d1+d2*d2+d3*d3;
      if(di<ds){ds=di;cell=fci;}
    }
    return cell;
  }


  private float[][] solveEigenproblems(double[][] a) {
    double[] e = new double[3];
    double[][] z = new double[3][3];
    Eigen.solveSymmetric33(a,z,e);
    float eui = (float)e[0];
    float evi = (float)e[1];
    float ewi = (float)e[2];
    float u1i = (float)z[0][0];
    float u2i = (float)z[0][1];
    float u3i = (float)z[0][2];
    if (ewi<0.0f) ewi = 0.0f;
    if (evi<ewi) evi = ewi;
    if (eui<evi) eui = evi;
    float[] es = new float[]{eui,evi,ewi};
    float[] us = new float[]{u1i,u2i,u3i};
    return new float[][]{us,es};
  }


  private float[][][] gaussWeights() {
    int n1 = (_w1+5)*2+1;
    int n2 = (_w2+5)*2+1;
    int n3 = (_w3+5)*2+1;
    float[][][] fx = new float[n3][n2][n1];
    fx[_w3+5][_w2+5][_w1+5] = 1f;
    float[][][] fs = new float[n3][n2][n1];
    float sigma = max(_w1,_w2,_w3)*2f;
    RecursiveGaussianFilterP rgf = new RecursiveGaussianFilterP(sigma);
    rgf.apply000(fx,fs);
    return copy(_w1+6,_w2+6,_w3+6,_w1+5,_w2+5,_w3+5,fs);
  }


  private FaultCell[] findCellsInBox(
    int b1, int e1, int b2, int e2, int b3, int e3, float fp, float ft)
  {
    ArrayList<FaultCell> fcl = new ArrayList<FaultCell>();
    for (int i3=b3; i3<=e3; ++i3) {
    for (int i2=b2; i2<=e2; ++i2) {
    for (int i1=b1; i1<=e1; ++i1) {
      FaultCell cell = _cells[i3][i2][i1];
      if (cell!=null&&cell.skin==null) {
        float dp = cell.fp-fp;
        float dt = abs(cell.ft-ft);
        dp = min(abs(dp),abs(dp+360.0f),abs(dp-360.0f));
        if (dp<20f && dt<15f) {fcl.add(cell);}
      }
    }}}
    return fcl.toArray(new FaultCell[0]);
  }

  // Methods to link mutually best nabors.
  private void linkAboveBelow(FaultCell ca, FaultCell cb) {
    cb.ca = ca;
    ca.cb = cb;
  }
  private void linkLeftRight(FaultCell cl, FaultCell cr) {
    cr.cl = cl;
    cl.cr = cr;
  }

  // Methods to find good nabors of a specified cell. These methods return
  // null if no nabor is good enough, based on various thresholds.
  private FaultCell findNaborAbove(FaultCellGrid cells, FaultCell cell) {
    FaultCell ca = cells.findCellAbove(cell);
    return canBeNabors(cell,ca)?ca:null;
  }
  private FaultCell findNaborBelow(FaultCellGrid cells, FaultCell cell) {
    FaultCell cb = cells.findCellBelow(cell);
    return canBeNabors(cell,cb)?cb:null;
  }
  private FaultCell findNaborLeft(FaultCellGrid cells, FaultCell cell) {
    FaultCell cl = cells.findCellLeft(cell);
    return canBeNabors(cell,cl)?cl:null;
  }
  private FaultCell findNaborRight(FaultCellGrid cells, FaultCell cell) {
    FaultCell cr = cells.findCellRight(cell);
    return canBeNabors(cell,cr)?cr:null;
  }

  // Returns true if two specified cells can be nabors. The two cells are
  // assumed to be within one sample of each other. This method uses other
  // attributes of the cells to determine whether or not they can be nabors.
  private boolean canBeNabors(FaultCell ca, FaultCell cb) {
    boolean can = true;
    if (ca==null || cb==null) {
      can = false;
    } else if (minFl(ca,cb)<_fllo) {
      can = false;
    } else if (minS1(ca,cb)<_fs1min) {
      can = false;
    } else if (maxS1(ca,cb)>_fs1max) {
      can = false;
    } else if (absDeltaFl(ca,cb)>_dflmax) {
      can = false;
    } else if (absDeltaFp(ca,cb)>_dfpmax) {
      can = false;
    } else if (absDeltaFt(ca,cb)>_dftmax) {
      can = false;
    } else if (absDeltaS1(ca,cb)>_ds1max) {
      can = false;
    } else if (maxDistanceToPlane(ca,cb)>_dnpmax) {
      can = false;
    }
    return can;
  }
  private static float minFl(FaultCell ca, FaultCell cb) {
    return min(ca.fl,cb.fl);
  }
  private static float minS1(FaultCell ca, FaultCell cb) {
    return min(ca.s1,cb.s1);
  }
  private static float maxS1(FaultCell ca, FaultCell cb) {
    return max(ca.s1,cb.s1);
  }
  private static float absDeltaFl(FaultCell ca, FaultCell cb) {
    return abs(ca.fl-cb.fl);
  }
  private static float absDeltaFp(FaultCell ca, FaultCell cb) {
    float del = ca.fp-cb.fp;
    return min(abs(del),abs(del+360.0f),abs(del-360.0f));
  }
  private static float absDeltaFt(FaultCell ca, FaultCell cb) {
    return abs(ca.ft-cb.ft);
  }
  private static float absDeltaS1(FaultCell ca, FaultCell cb) {
    return abs(ca.s1-cb.s1);
  }
  private static float maxDistanceToPlane(FaultCell ca, FaultCell cb) {
    float aw1 = ca.w1, aw2 = ca.w2, aw3 = ca.w3;
    float ax1 = ca.x1, ax2 = ca.x2, ax3 = ca.x3;
    float bw1 = cb.w1, bw2 = cb.w2, bw3 = cb.w3;
    float bx1 = cb.x1, bx2 = cb.x2, bx3 = cb.x3;
    float dx1 = ax1-bx1;
    float dx2 = ax2-bx2;
    float dx3 = ax3-bx3;
    float dab = aw1*dx1+aw2*dx2+aw3*dx3;
    float dba = bw1*dx1+bw2*dx2+bw3*dx3;
    return max(abs(dab),abs(dba));
  }

  // Rotates a specified point by strike (phi) and dip (theta) angles,
  // given specified cosines (cp and ct) and sines (sp and st) of those 
  // angles. The order of transformation is
  // (1) rotate around axis x3 by dip angle
  // (2) rotate around axis x1 by strike angle
  // Returns the coordinates of the rotated point.
  private static float[] rotatePoint(
      float cp, float sp, float ct, float st, float[] x) {
    float x1 = x[0], x2 = x[1], x3 = x[2];
    float y1 =     ct*x1+   st*x2;
    float y2 = -cp*st*x1+cp*ct*x2+sp*x3;
    float y3 =  sp*st*x1-sp*ct*x2+cp*x3;
    return new float[]{y1,y2,y3};
  }

  private static void trace(String s) {
    System.out.println(s);
  }

  private static class FloatList {
    public int n;
    public float[] a = new float[1024];
    public void add(float f) {
      if (n==a.length) {
        float[] t = new float[2*n];
        System.arraycopy(a,0,t,0,n);
        a = t;
      }
      a[n++] = f;
    }
    public void add(float[] f) {
      int m = f.length;
      for (int i=0; i<m; ++i)
        add(f[i]);
    }
    public float[] trim() {
      if (n==0)
        return null;
      float[] t = new float[n];
      System.arraycopy(a,0,t,0,n);
      return t;
    }
  }
}
