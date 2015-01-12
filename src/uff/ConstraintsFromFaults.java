/****************************************************************************
Copyright (c) 2013, Colorado School of Mines and others. All rights reserved.
This program and accompanying materials are made available under the terms of
the Common Public License - v1.0, which accompanies this distribution, and is 
available at http://www.eclipse.org/legal/cpl-v10.html
****************************************************************************/
package uff;

import ifs.*;
import java.util.*;
import static edu.mines.jtk.util.ArrayMath.*;

/**
 * Set weights and constraints for flattening: 
 * set zero weights at fault, 
 * set hard constraints using fault slip vectors.
 * @author Xinming Wu
 * @version 2014.09.18
 */

public class ConstraintsFromFaults {

  public ConstraintsFromFaults(
    FaultSkin[] fss, float[][][] w) {
    _w = w;
    _fss = fss;
    _n3 = w.length;
    _n2 = w[0].length;
    _n1 = w[0][0].length;
    _mk = new int[_n3][_n2][_n1];
    _fm = new int[_n3][_n2][_n1];
    faultMap(_fm);
  }

  public float[][][] getWeightsAndConstraints(
    float[][][] ws, float[][][] cp) 
  {
    setWeightsOnFault(ws);
    ArrayList<float[][]> cl = new ArrayList<float[][]>();
    for (FaultSkin fs:_fss) {
      for (FaultCell fc:fs) {
        float[] cx = fc.getX();
        float[] cw = fc.getW();
        float[] cs = fc.getS();
        float[] fx = new float[3];
        float[] hx = new float[3];
        float xs1 = cx[0]+cs[0];
        float xs2 = cx[1]+cs[1];
        float xs3 = cx[2]+cs[2];
        fx[0]=hx[0]=bound1(round(xs1));
        fx[1]=hx[1]=bound2(round(xs2));
        fx[2]=hx[2]=bound3(round(xs3));
        if(!nearestFaultCell(hx)){hx=copy(fx);}
        boolean valid = false;
        float w2 = abs(cw[1]), w3 = abs(cw[2]);
        if (w2>w3) {valid = shift2(2.0f,cw[1],fx,hx);} 
        else       {valid = shift3(2.0f,cw[2],fx,hx);}
        if(valid) {
          onFault(fx,ws); onFault(hx,ws);
          cl.add(new float[][]{fx,hx,mul(cs,0.5f)});
          addPoints(fx,hx,cp);
        }
      }
    }
    int ns = cl.size();
    System.out.println("sets of control points:"+ns);
    float[][][] cs = new float[3][ns][3];
    for (int is=0; is<ns; ++is) {
      float[][] ps = cl.get(is);
      for (int ip=0; ip<3; ++ip) {
        cs[0][is][ip] = ps[ip][0];
        cs[1][is][ip] = ps[ip][1];
        cs[2][is][ip] = ps[ip][2];
      }
    }
    return cs;
  }

  public float[][][] getScreenPoints(float[][][] ws, float[][][] cp) {
    setWeightsOnFault(ws);
    float[][] fl = normalizeFl();
    ArrayList<float[][]> cl = new ArrayList<float[][]>();
    int nk = _fss.length;
    for (int ik=0; ik<nk; ++ik) {
      FaultCell[] fcs = FaultSkin.getCells(new FaultSkin[]{_fss[ik]});
      int nc = fcs.length;
      for (int ic=0; ic<nc; ++ic) {
      FaultCell fc = fcs[ic];
      float[] cx = fc.getX();
      float[] cw = fc.getW();
      float[] cs = fc.getS();
      float[] fx = new float[3];
      float[] hx = new float[3];
      float[] xf = new float[3];
      xf[0] = fl[ik][ic];
      xf[1] = fl[ik][ic];
      xf[2] = fl[ik][ic];
      float xs1 = cx[0]+cs[0];
      float xs2 = cx[1]+cs[1];
      float xs3 = cx[2]+cs[2];
      fx[0]=hx[0]=bound1(round(xs1));
      fx[1]=hx[1]=bound2(round(xs2));
      fx[2]=hx[2]=bound3(round(xs3));
      if(!nearestFaultCell(hx)){hx=copy(fx);}
      boolean valid = false;
      float w2 = abs(cw[1]);
      float w3 = abs(cw[2]);
      if (w2>w3) {valid = shift2(2.0f,cw[1],fx,hx);} 
      else       {valid = shift3(2.0f,cw[2],fx,hx);}
      if(valid) {
        onFault(fx,ws);
        onFault(hx,ws);
        cl.add(new float[][]{fx,hx,cs,xf});
        addPoints(fx,hx,cp);
      }
      }
    }
    int ns = cl.size();
    System.out.println("sets of control points:"+ns);
    float[][][] cs = new float[4][3][ns];
    for (int is=0; is<ns; ++is) {
      float[][] ps = cl.get(is);
      cs[0][0][is] = ps[0][0];
      cs[0][1][is] = ps[0][1];
      cs[0][2][is] = ps[0][2];

      cs[1][0][is] = ps[1][0];
      cs[1][1][is] = ps[1][1];
      cs[1][2][is] = ps[1][2];

      cs[2][0][is] = ps[2][0];
      cs[2][1][is] = ps[2][1];
      cs[2][2][is] = ps[2][2];

      cs[3][0][is] = ps[3][0];
      cs[3][1][is] = ps[3][1];
      cs[3][2][is] = ps[3][2];
    }
    return cs;
  }

  private float[][] normalizeFl() {
    int ns = _fss.length;
    float[][] fl = new float[ns][];
    for (int is=0; is<ns; ++is) {
      FaultCell[] fc = FaultSkin.getCells(new FaultSkin[]{_fss[is]});
      int nc = fc.length;
      fl[is] = new float[nc];
      for (int ic=0; ic<nc; ++ic) {
        fl[is][ic] = fc[ic].getFl();
      }
      div(fl[is],max(fl[is]),fl[is]);
    }
    return fl;
  }

  private boolean moveToNearestCell(float[] x) {
    float x1 = x[0];
    float x2 = x[1];
    float x3 = x[2];
    float[][] xp = setKdTreePoints(x1);
    if(xp==null) return false;
    KdTree kt = new KdTree(xp);
    int id = kt.findNearest(new float[]{x2,x3});
    float p2 = xp[0][id]; 
    float p3 = xp[1][id];
    float d2 = x2-p2; 
    float d3 = x3-p3;
    float ds = sqrt(d2*d2+d3*d3);
    if(ds>5f){return false;}
    else     {x[1]=p2;x[2]=p3;return true;}
  }

  private float[][] setKdTreePoints(float x1) {
    int i1 = round(x1);
    HashSet<Integer> x2 = new HashSet<Integer>();
    HashSet<Integer> x3 = new HashSet<Integer>();
    for (int i3=0; i3<_n3; ++i3) 
    for (int i2=0; i2<_n2; ++i2) 
      if(_fm[i3][i2][i1]==1) {
        x2.add(i2); x3.add(i3);
      }
    int np = x2.size();
    if(np==0) return null;
    float[][] xp = new float[2][np];
    int k2 = 0;
    for (int i2:x2) {xp[0][k2]=i2;k2++;}
    int k3 = 0;
    for (int i3:x3) {xp[1][k3]=i3;k3++;}
    return xp;
  }

  private boolean nearestFaultCell(float[] xi) {
    int d = 4;
    float ds = 500;
    int i1 = round(xi[0]);
    int i2 = round(xi[1]);
    int i3 = round(xi[2]);
    for (int d3=-d; d3<=d; ++d3) {
      for (int d2=-d; d2<=d; ++d2) {
        int x2 = bound2(i2+d2);
        int x3 = bound3(i3+d3);
        if(_fm[x3][x2][i1]==1) {
          float dsi = d2*d2+d3*d3;
          if(dsi<ds) {
            ds = dsi;
            xi[0] = i1;
            xi[1] = x2;
            xi[2] = x3;
          }
        }
      }
    }
    if(ds<500.0f) {return true;}
    else          {return false;}
  }


  private int bound1(int x) {
    if(x<0)   {return 0;}
    if(x>=_n1){return _n1-1;}
    return x;
  }
  private int bound2(int x) {
    if(x<0)   {return 0;}
    if(x>=_n2){return _n2-1;}
    return x;
  }
  private int bound3(int x) {
    if(x<0)   {return 0;}
    if(x>=_n3){return _n3-1;}
    return x;
  }

  public void faultMap(int[][][] fm) {
    for (FaultSkin fs:_fss) {
      for (FaultCell fc:fs) {
        float[] x = fc.getX();
        int x1 = round(x[0]);
        int x2 = round(x[1]);
        int x3 = round(x[2]);
        fm[x3][x2][x1] = 1;
      }
    }
  }

  public int[][][] getFaultMap() {
    int fn = _fss.length;
    int[][][] fm = new int[3][fn][]; 
    for (int fi=0; fi<fn; ++fi) { 
      FaultCell[] fcs = _fss[fi].getCells();
      int nc = fcs.length;
      fm[0][fi] = new int[nc];
      fm[1][fi] = new int[nc];
      fm[2][fi] = new int[nc];
      for (int ic=0; ic<nc; ++ic) { 
        float[] xc = fcs[ic].getX();
        fm[0][fi][ic] = round(xc[0]);
        fm[1][fi][ic] = round(xc[1]);
        fm[2][fi][ic] = round(xc[2]);
      }
    }
    return fm;
  }

  private void addPoints(float[] c, float[] k, float[][][] cp) {
    int c1 = round(c[0]);
    int c2 = round(c[1]);
    int c3 = round(c[2]);
    int k1 = round(k[0]);
    int k2 = round(k[1]);
    int k3 = round(k[2]);
    cp[c3][c2][c1] = 1.0f;
    cp[k3][k2][k1] = 1.0f;
  }

  private boolean shift2(float sc, float w2, float[] c, float[] k) {
    float sn2 = (w2<0.f)?-1.f:1.f;
    float ds2 = sn2*sc;
    float ds1 = sn2*0.0f;

    c[1] -= ds2;
    c[2] -= ds1;
    int c1 = round(c[0]);
    int c2 = round(c[1]);
    int c3 = round(c[2]);
    if(onBound(c1,c2,c3)){return false;}

    k[1] +=ds2;
    k[2] +=ds1;
    int k1 = round(k[0]);
    int k2 = round(k[1]);
    int k3 = round(k[2]);
    if(onBound(k1,k2,k3)){return false;}
    _mk[c3][c2][c1] += 1;
    if(_mk[c3][c2][c1]>1) {return false;}

    _mk[k3][k2][k1] += 1;
    if(_mk[k3][k2][k1]>1) {return false;}
    return true;
  }

  private boolean shift3(float sc, float w3, float[] c, float[] k) {
    float sn3 = (w3<0.f)?-1.f:1.f;
    float ds3 = sn3*sc;
    float ds1 = sn3*0.0f;

    c[1] -= ds1;
    c[2] -= ds3;
    int c1 = round(c[0]);
    int c2 = round(c[1]);
    int c3 = round(c[2]);
    if(onBound(c1,c2,c3)){return false;}

    k[1] += ds1;
    k[2] += ds3;
    int k1 = round(k[0]);
    int k2 = round(k[1]);
    int k3 = round(k[2]);
    if(onBound(k1,k2,k3)){return false;}
    _mk[c3][c2][c1] += 1;
    if(_mk[c3][c2][c1]>1) {return false;}
    _mk[k3][k2][k1] += 1;
    if(_mk[k3][k2][k1]>1) {return false;}
    return true;
  }


  private void setWeightsOnFault(float[][][] ws) {
    int n3 = ws.length;
    int n2 = ws[0].length;
    int m2 = n2-1;
    int m3 = n3-1;
    for (FaultSkin fs:_fss) {
      for (FaultCell fc:fs) {
        float[] pi = fc.getX();
        int pi1 = round(pi[0]);
        int pi2 = round(pi[1]);
        int pi3 = round(pi[2]);
        int p2m = pi2-1;if(p2m<0) {p2m=0;}
        int p2p = pi2+1;if(p2p>m2){p2p=m2;}
        int p3m = pi3-1;if(p3m<0) {p3m=0;}
        int p3p = pi3+1;if(p3p>m3){p3p=m3;}
        ws[pi3][pi2][pi1] = 0.0f;
        ws[p3m][pi2][pi1] = 0.0f;
        ws[pi3][p2m][pi1] = 0.0f;
        ws[p3p][pi2][pi1] = 0.0f;
        ws[pi3][p2p][pi1] = 0.0f;
      }
    }
  }

  private void onFault(float[] p, float[][][] w) {
    int i1 = round(p[0]);
    int i2 = round(p[1]);
    int i3 = round(p[2]);
    float wi = w[i3][i2][i1];
    if (wi==0.0f){w[i3][i2][i1]=0.1f;} 
  }

  private boolean onBound(int p1, int p2, int p3) {
    if(p1<0||p1>=_n1){return true;}
    if(p2<0||p2>=_n2){return true;}
    if(p3<0||p3>=_n3){return true;}
    return false;
  }


  private int _n1,_n2,_n3;
  private FaultSkin[] _fss;
  private float[][][] _w = null;
  private int[][][] _mk = null;
  private int[][][] _fm = null;
}

