from utils2 import *

setupForSubset("pik")
pngDir = getPngDir()
s1,s2 = getSamplings()
n1,n2 = s1.count,s2.count

fxfile = "pik" # input attribute
fxfile = "fxs" # input attribute

def main(args):
  #goPik()
  #goSeisPik()
  goSeisPikWithWrongSource()
def goSeisPik():
  gx = readImage(fxfile)
  sp = SaltPicker2()
  fx = sp.applyForInsAmp(gx)
  fx = sub(fx,min(fx))
  fx = div(fx,max(fx))
  opp = OptimalPathPicker(4,3)
  wht = opp.applyForWeight(transpose(fx))
  tms1 = zerofloat(n2,n1)
  tms2 = zerofloat(n2,n1)
  pik1 = opp.forwardPick(100,wht,tms1)
  pik2 = opp.backwardPick(round(pik1[n2-1]),wht,tms2)
  x2 = rampfloat(0,1,n2)
  tt = transpose(tms1)
  gx = div(gx,max(gx))
  plot(gx,cmin=-1,cmax=1.0,w1=250,w2=1000,clab="Amplitude",png="seis")
  plot(fx,cmin=0.1,cmax=1.0,w1=250,w2=1000,clab="Amplitude",png="env")
  plot(tt,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt),contour=True,w1=250,w2=1000,
       clab="Travel time",png="time")
  plot(tt,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt),contour=True,
          xp=[pik2,x2],w1=250,w2=1000,clab="Travel time",png="timePik")
  plot(fx,cmin=0.1,cmax=1.0,xp=[pik2,x2],
        w1=250,w2=1000,clab="Amplitude",png="seisPik")
def goSeisPikWithWrongSource():
  gx = readImage(fxfile)
  sp = SaltPicker2()
  fx = sp.applyForInsAmp(gx)
  fx = sub(fx,min(fx))
  fx = div(fx,max(fx))
  opp = OptimalPathPicker(4,3)
  wht = opp.applyForWeight(transpose(fx))
  tms1 = zerofloat(n2,n1)
  tms2 = zerofloat(n2,n1)
  pik1 = opp.forwardPick(0,wht,tms1)
  pik2 = opp.backwardPick(round(pik1[n2-1]),wht,tms2)
  x2 = rampfloat(0,1,n2)
  tt1 = transpose(tms1)
  tt2 = transpose(tms2)
  gx = div(gx,max(gx))
  #plot(gx,cmin=-1,cmax=1.0,w1=250,w2=1000,clab="Amplitude",png="seis")
  #plot(fx,cmin=0.1,cmax=1.0,w1=250,w2=1000,clab="Amplitude",png="env")
  plot(tt1,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt1),contour=True,w1=250,w2=1000,
       clab="Travel time",png="time1")
  plot(tt2,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt2),contour=True,w1=250,w2=1000,
       clab="Travel time",png="time2")

  plot(tt1,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt1),contour=True,
          xp=[pik1,x2],w1=250,w2=1000,clab="Travel time",png="timePik1")
  plot(tt2,cmap=ColorMap.JET,cmin=0.1,cmax=max(tt2),contour=True,
          xp=[pik2,x2],w1=250,w2=1000,clab="Travel time",png="timePik2")
  plot(fx,cmin=0.1,cmax=1.0,xp=[pik1,x2],
        w1=250,w2=1000,clab="Amplitude",png="seisPik1")
  plot(fx,cmin=0.1,cmax=1.0,xp=[pik2,x2],
        w1=250,w2=1000,clab="Amplitude",png="seisPik2")

def goPik():
  fx = readImageL(fxfile)
  fx = sub(fx,min(fx))
  fx = div(fx,max(fx))
  fx = pow(fx,0.5)
  fx = sub(fx,min(fx))
  fx = div(fx,max(fx))
  opp = OptimalPathPicker(3,1)
  wht = opp.applyForWeight(fx)
  tms1 = zerofloat(n1,n2)
  tms2 = zerofloat(n1,n2)
  pik1 = opp.forwardPick(50,wht,tms1)
  pik2 = opp.backwardPick(round(pik1[n1-1]),wht,tms2)
  x1 = rampfloat(0,1,n1)
  ft = transpose(fx)
  tt = transpose(tms1)
  plot(ft,cmin=0.1,cmax=1.0,w1=180,w2=750,clab="Attribute",png="map")
  plot(tt,cmin=0.1,cmax=max(tt),contour=True,w1=180,w2=750,
       clab="Travel time",png="time")
  plot(tt,cmin=0.1,cmax=max(tt),contour=True,
          xp=[pik2,x1],w1=180,w2=750,clab="Travel time",png="timePik")
  plot(ft,cmin=0.1,cmax=1.0,xp=[pik2,x1],
        w1=180,w2=750,clab="Attribute",png="mapPik")

def goBag2dTwo():
  global pngDir
  setupForSubset("bag2dTwo")
  pngDir = getPngDir()
  s1,s2 = getSamplings()
  n1,n2 = s1.count,s2.count
  gx = readImage(fxfile)
  gx = gain(gx)
  c1 = [280,250,380,270,360,250,530,150, 70,600,570,790,640,720,600,280]
  c2 = [  0, 80,210,260,310,390,470,600,950,930,660,420,300,200,  0,  0]
  sp = SaltPicker2()
  pa = sp.applyForInsAmp(gx)
  pm = max(pa)/4
  for i1 in range(n1):
    pa[0   ][i1] = pm
    pa[n2-1][i1] = pm
  xu = sp.initialBoundary(1,c1,c2)
  xt = copy(xu)
  #xu = sp.regridBoundary(1,[c1,c2])
  w1 = round(n1*0.5)
  w2 = round(n2*0.5)
  nr,dr = 65,1
  bs = sp.refine(nr,1,10,2,xu,pa)
  #xu = sp.pickNext(10,1,5,2,xu[0],xu[1],pa)
  plot(gx,cmin=-2,cmax=2,w1=w1,w2=w2,png="seis")
  plot(pa,cmin=0,cmax=1,pp=[c1,c2],w1=w1,w2=w2,png="initial")
  plot(pa,cmin=0,cmax=1,xp=[c1,c2],w1=w1,w2=w2,)
  plot(pa,cmin=0,cmax=1,xp=[c1,c2],xu=xt,nr=nr,w1=w1,w2=w2,png="band")
  plot(gx,cmin=-2,cmax=2,xp=[xu[0],xu[1]],w1=w1,w2=w2,png="final")
  opp = OptimalPathPicker(10,2)
  ft = opp.applyTransform(bs)
  m2,m1 = len(bs),len(bs[0])
  wht = opp.applyForWeight(ft)
  tms1 = zerofloat(m2,m1)
  tms2 = zerofloat(m2,m1)
  pik1 = opp.forwardPick(nr,wht,tms1)
  pik2 = opp.backwardPick(round(pik1[m2-1]),wht,tms2)
  x2 = rampfloat(0,1,m2)
  plot(bs,cmin=0.05,cmax=0.9,w1=180,w2=1200,png="bandMap")
  plot(bs,cmin=0.05,cmax=0.9,xp=[pik2,x2],w1=180,w2=1200,png="bandPik")
def goBagDlsp():
  fx = readImage(fxfile)
  fx = gain(fx)
  c1 = [280,250,380,270,360,250,530,150, 70,600,570,790,640,720,600,280]
  c2 = [  0, 80,210,260,310,390,470,600,950,930,660,420,300,200,  0,  0]
  sp = SaltPicker2()
  xu = sp.initialBoundary(1,c1,c2)
  gx = zerofloat(n1,n2)
  p1 = 0
  p2 = 0
  np = len(xu[0])
  for pk in range(np):
    i1 = round(xu[0][pk])
    i2 = round(xu[1][pk])
    i1 = min(i1,n1-1)
    i2 = min(i2,n2-1)
    i1 = max(i1,0)
    i2 = max(i2,0)
    gx[i2][i1] = 1
    p1 += i1
    p2 += i2
  c1 = [round(p1/np)+150]
  c2 = [round(p2/np)]
  r1 = [10]
  r2 = [10]
  '''
  for i2 in range(200,220,1):
    gx[i2][400] = 1
    gx[i2][420] = 1
  for i1 in range(400,420,1):
    gx[200][i1] = 1
    gx[220][i1] = 1
  c1 = [410]
  c2 = [210]
  r1 = [40]
  r2 = [40]
  '''
  mu,lamda,alpha = 0.2,5,-100
  ls = LevelSet2(mu,lamda,alpha,3,1,100)
  gx = ls.toGrayFloats(gx)
  rgf = RecursiveGaussianFilter(2)
  rgf.apply00(gx,gx)
  gs = div(1,add(1,mul(gx,gx)))
  plot(gx)
  ph = ls.initialLevelSet(n1,n2,c1,c2,r1,r2,2)
  ph0 = copy(ph)
  ls.updateLevelSetP(1.5,gs,ph)
  plot(gx,phi=ph0)
  plot(gx,phi=ph)
  plot(ph,cmin=-2,cmax=2)

def goBag2dThree():
  gx = readImage(fxfile)
  gx = gain(gx)
  c1 = [145,130,250,220,410,250,450, 330, 720,600,812,520,620,400,145]
  c2 = [  0,110,380,500,660,780,880,1128,1080,900,731,285,240,  0,  0]
  sp = SaltPicker2()
  pa = sp.applyForInsAmp(gx)
  pm = max(pa)/2
  for i1 in range(n1):
    pa[0   ][i1] = pm
    pa[n2-1][i1] = pm
  #xu = sp.initialBoundary(1,c1,c2)
  xu = sp.regridBoundary(1,[c1,c2])
  plot(gx,cmin=-2,cmax=2,png="seis")
  plot(gx,cmin=-2,cmax=2,pp=[c1,c2],png="initial")
  plot(pa,cmin=0,cmax=2,xp=[xu[0],xu[1]])
  plot(pa,cmin=0,cmax=2,xu=xu)
  bs = sp.refine(95,1,40,2,xu,pa)
  plot(gx,cmin=-2,cmax=2,xp=[xu[0],xu[1]],png="final")
  opp = OptimalPathPicker(40,2)
  ft = opp.applyTransform(bs)
  m2,m1 = len(bs),len(bs[0])
  wht = opp.applyForWeight(ft)
  tms1 = zerofloat(m2,m1)
  tms2 = zerofloat(m2,m1)
  pik1 = opp.forwardPick(100,wht,tms1)
  pik2 = opp.backwardPick(round(pik1[m2-1]),wht,tms2)
  x2 = zerofloat(m2)
  for i2 in range(m2):
    x2[i2]=i2
  plot(bs,cmin=0.01,cmax=0.5,w1=200,w2=2400)
  plot(bs,cmin=0.01,cmax=0.5,xp=[pik2,x2],w1=200,w2=2400)

def gain(x):
  g = mul(x,x) 
  n2 = len(x)
  n1 = len(x[0])
  ref = RecursiveExponentialFilter(50.0)
  ref.apply1(g,g)
  y = zerofloat(n1,n2)
  div(x,sqrt(g),y)
  return y

def smooth(sig1,sig2,sigs,x):
  n2 = len(x)
  n1 = len(x[0])
  lof = LocalOrientFilter(sig1,sig2)
  ets = lof.applyForTensors(x)
  ets.setEigenvalues(0.001,1.0)
  lsf = LocalSmoothingFilter()
  g = zerofloat(n1,n2)
  lsf.apply(ets,sigs,x,g)
  return g

#############################################################################
# graphics

gray = ColorMap.GRAY
cjet = ColorMap.JET
alpha = fillfloat(1.0,256); alpha[0] = 0.0
ajet = ColorMap.setAlpha(cjet,alpha)

def plot(f,xp=None,pp=None,xs=None,xu=None,nr=50,phi=None,v1=None,v2=None,
        cmap=gray,cmin=None,cmax=None,perc=None,contour=False,
        w1=None,w2=None,clab=None,png=None): 
  orientation = PlotPanel.Orientation.X1DOWN_X2RIGHT;
  panel = PlotPanel(1,1,orientation);
  #panel.setVInterval(0.2)
  n2 = len(f)
  n1 = len(f[0])
  s2 = Sampling(n2,1,0)
  s1 = Sampling(n1,1,0)
  panel.setHLabel("Inline (traces)")
  panel.setVLabel("Depth (samples)")
  ft = fillfloat(-10,n1,n2)
  panel.setHLimits(0,0,s2.last)
  panel.setVLimits(0,0,s1.last)
  pxv = panel.addPixels(0,0,s1,s2,f);
  #pxv.setInterpolation(PixelsView.Interpolation.LINEAR)
  pxv.setInterpolation(PixelsView.Interpolation.NEAREST)
  pxv.setColorModel(cmap)
  if perc:
    print perc
    pxv.setPercentiles(100-perc,perc)
  if cmin and cmax:
    pxv.setClips(cmin,cmax)
  #panel.setTitle("normal vectors")
  if contour:
    cv = panel.addContours(s1,s2,f)
    cv.setContours(120)
    #cv.setColorModel(ColorMap.JET)
    cv.setLineWidth(3.0)
    cv.setLineColor(Color.BLACK)
  if phi:
    cv = panel.addContours(phi)
    cv.setContours([0])
    cv.setLineColor(Color.RED)
    cv.setLineWidth(1.0)
  if xp:
    ptv = panel.addPoints(0,0,xp[0],xp[1])
    ptv.setLineColor(Color.YELLOW)
    ptv.setLineWidth(5.0)
  if xu:
    np = len(xu[0])
    for ip in range(np):
      x1c = xu[0][ip]
      x2c = xu[1][ip]
      u1c = xu[2][ip]
      u2c = xu[3][ip]
      x1m = x1c-u1c*nr
      x2m = x2c-u2c*nr
      x1p = x1c+u1c*nr
      x2p = x2c+u2c*nr
      x1s = [x1m,x1c,x1p]
      x2s = [x2m,x2c,x2p]
      ptv2 = panel.addPoints(x1s,x2s)
      ptv2.setLineColor(Color.YELLOW)
      ptv2.setLineWidth(1.0)
    if xp:
      ptv = panel.addPoints(0,0,xp[0],xp[1])
      ptv.setLineColor(Color.RED)
      ptv.setLineWidth(3.0)
  if pp:
    ptvl = panel.addPoints(0,0,pp[0],pp[1])
    ptvl.setLineColor(Color.RED)
    ptvl.setLineWidth(3.0)
    ptvp = panel.addPoints(0,0,pp[0],pp[1])
    ptvp.setLineStyle(PointsView.Line.NONE)
    ptvp.setMarkStyle(PointsView.Mark.HOLLOW_CIRCLE)
    #ptvp.setMarkStyle(PointsView.Mark.CROSS)
    ptvp.setMarkColor(Color.YELLOW)
    ptvp.setMarkSize(12.0)
    ptvp.setLineWidth(3.0)
  if xs:
    for ip in range(len(xs)):
      ptv = panel.addPoints(xs[ip][0],xs[ip][1])
      ptv.setLineStyle(PointsView.Line.NONE)
      ptv.setMarkStyle(PointsView.Mark.FILLED_CIRCLE)
      ptv.setMarkColor(Color.RED)
      ptv.setMarkSize(2.0)
  if(clab):
    cb = panel.addColorBar();
    cb.setLabel(clab)
  panel.setColorBarWidthMinimum(50)
  moc = panel.getMosaic();
  frame = PlotFrame(panel);
  frame.setDefaultCloseOperation(PlotFrame.EXIT_ON_CLOSE);
  frame.setVisible(True);
  if w1 and w2:
    frame.setSize(w2,w1)
  else:
    frame.setSize(round(n2*0.8),round(n1*0.8))
  #frame.setSize(1190,760)
  frame.setFontSize(14)
  if pngDir and png:
    frame.paintToPng(1080,3.333,pngDir+png+".png")

#############################################################################
# Run the function main on the Swing thread
import sys
class _RunMain(Runnable):
  def __init__(self,main):
    self.main = main
  def run(self):
    self.main(sys.argv)
def run(main):
  SwingUtilities.invokeLater(_RunMain(main)) 
run(main)
