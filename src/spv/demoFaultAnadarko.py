#############################################################################
"""
Demo of dynamic warping for automatic picking
Author: Xinming Wu, University of Texas at Austin
Version: 2016.06.01
"""


from utils import * 
setupForSubset("anadarko")
s1,s2,s3 = getSamplings()
n1,n2,n3 = s1.count,s2.count,s3.count
f1,f2,f3 = s1.getFirst(),s2.getFirst(),s3.getFirst()
d1,d2,d3 = s1.getDelta(),s2.getDelta(),s3.getDelta()
#############################################################################
gxfile = "xs" # input semblance image
epfile = "ep"  # planarity
effile = "ef"  # 1-planarity
fefile = "fe"  # 1-planarity
flfile = "fl"  # fault likelihood
fpfile = "fp"  # fault strike;
ftfile = "ft"  # fault dip;
fvfile = "fv"  # fault dip;
vpfile = "vp"  # fault dip;
vtfile = "vt"  # fault dip;
fetfile = "fet" # fault likelihood thinned
fptfile = "fpt" # fault strike thinned
fttfile = "ftt" # fault dip thinned
fskfile = "skin"

pngDir = None
pngDir = getPngDir()
plotOnly = True
# These parameters control the scan over fault strikes and dips.
# See the class FaultScanner for more information.
minTheta,maxTheta = 65,80
minPhi,maxPhi = 0,360
sigmaPhi,sigmaTheta=6,12

def main(args):
  #goFaultAttribute()
  #goFaultOrientScan()
  #goSurfaceVoting()
  goFaultSurfaces()
  #goFaultLikelihood()
  #goFaultSkins()

def goFaultAttribute():
  '''
  ep = readImage3DL(epfile)
  hp = Helper()
  ec = hp.convert(ep)
  writeImage(epfile,ec)
  '''
  ep = readImage3D(epfile)
  rgf = RecursiveGaussianFilter(3)
  rgf.apply000(ep,ep)
  plot3(ep,ep,cmin=0.01,cmax=1.0,cmap=jetFill(1.0),
      clab="Fault Attribute")

def goFaultOrientScan():
  gx = readImage3D(epfile)
  ep = readImage3D(epfile)
  ep2 = zerofloat(n1,n2,n3)
  ep3 = zerofloat(n1,n2,n3)
  rgf1 = RecursiveGaussianFilter(2)
  rgf2 = RecursiveGaussianFilter(2)
  rgf3 = RecursiveGaussianFilter(2)
  rgf2.apply0XX(ep,ep) 
  rgf1.applyX2X(ep,ep2)
  rgf1.applyXX2(ep,ep3)
  ep2 = mul(ep2,ep2)
  ep3 = mul(ep3,ep3)
  eps = sqrt(add(ep2,ep3))
  rgf3.apply000(eps,eps) 
  eps = sub(eps,min(eps))
  eps = div(eps,max(eps))
  eps = pow(eps,0.4)
  ep = sub(1,eps)
  print max(ep)
  print min(ep)
  plot3(eps,cmin=0.1,cmax=1)
  fos = FaultOrientScanner3(sigmaPhi,sigmaTheta)
  if not plotOnly:
    fe,fp,ft = fos.scan(minPhi,maxPhi,minTheta,maxTheta,ep)
    fet,fpt,ftt=fos.thin([fe,fp,ft])
    writeImage(fefile,fe)
    writeImage(fetfile,fet)
    writeImage(fptfile,fpt)
    writeImage(fttfile,ftt)
  else:
    fe = readImage3D(fefile)
    fp = readImage3D(fpfile)
  print min(fe) 
  print max(fe) 
  ep = sub(1,pow(ep,8))
  plot3(gx,ep,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Planarity",png="ep")
  plot3(gx,fe,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Ray tracing",png="fl")
  plot3(gx,fp,cmin=1,cmax=360,cmap=jetFill(1.0),
      clab="Fault strike (degrees)",png="ph")

def goSurfaceVoting():
  gx = readImage3D(epfile)
  osv = OptimalSurfaceVoterP(10,20,30)
  if not plotOnly:
    fet = readImage3D(fetfile)
    fpt = readImage3D(fptfile)
    ftt = readImage3D(fttfile)
    osv.setStrainMax(0.25,0.25)
    osv.setSurfaceSmoothing(2,2)
    fv,vp,vt = osv.applyVoting(4,0.3,fet,fpt,ftt)
    writeImage("vp",vp)
    writeImage("vt",vt)
    writeImage(fvfile,fv)
  else:
    fv = readImage3D(fvfile)
    vp = readImage3D(vpfile)
    vt = readImage3D(vtfile)
  '''
  plot3(gx,ep,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Planarity",png="ep")
  '''
  plot3(gx,fv,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Surface voting",png="fv")
  plot3(gx,fvp,cmin=1,cmax=180,cmap=jetFill(1.0),
      clab="Fault strike (degrees)",png="ph")

def goFaultSurfaces():
  gx = readImage3D(epfile)
  osv = OptimalSurfaceVoterP(10,20,30)
  if not plotOnly:
    fv = readImage3D(fvfile)
    vp = readImage3D(vpfile)
    vt = readImage3D(vtfile)
    u1 = zerofloat(n1,n2,n3)
    u2 = zerofloat(n1,n2,n3)
    u3 = zerofloat(n1,n2,n3)
    ep = zerofloat(n1,n2,n3)
    lof = LocalOrientFilter(4,2,2)
    lof.applyForNormalPlanar(fv,u1,u2,u3,ep)
    ft,pt,tt = osv.thin([fv,vp,vt])
    fsk = FaultSkinner()
    fsk.setGrowing(5,0.2)
    seeds = fsk.findSeeds(10,0.8,ep,ft,pt,tt)
    skins = fsk.findSkins(0.8,2000,seeds,fv,vp,vt)
    for skin in skins:
      skin.smooth(5)
    removeAllSkinFiles(fskfile)
    writeSkins(fskfile,skins)
  else:
    fv = readImage3D(fvfile)
    skins = readSkins(fskfile)
  for skin in skins:
    skin.updateStrike()
  fs = zerobyte(n1,n2,n3)
  ff = zerofloat(n1,n2,n3)
  osv.getFaultValues(skins,fs)
  writeImage("fs",fs)
  for i3 in range(n3):
    for i2 in range(n2):
      for i1 in range(n1):
        ff[i3][i2][i1] = fs[i3][i2][i1]
  print max(fs)
  plot3(gx,clab="Amplitude",png="seis")
  plot3(gx,fv,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Voting score",png="fv")
  plot3(gx,ff,cmin=1,cmax=max(fs),cmap=jetFillExceptMin(1.0),
      clab="Fault mark",png="fs")
  plot3(gx,fv,cmin=0.0,cmax=180,cmap=hueFill(1.0),
      clab="Fault strike",png="fp")
  plot3(gx,skins=skins)
  plot3(gx,skinx=skins,au=80,ev=55,v3=0.05,png="skinv")

def goFaultLikelihood():
  minPhi,maxPhi = 0,360
  minTheta,maxTheta = 70,85
  sigmaPhi,sigmaTheta = 12,20
  gx = readImage3D(gxfile)
  gx = gain(gx)
  if not plotOnly:
    sigma1,sigma2,sigma3,pmax = 16.0,1.0,1.0,5.0
    p2,p3,ep = FaultScanner.slopes(sigma1,sigma2,sigma3,pmax,gx)
    gx = FaultScanner.taper(10,0,0,gx)
    fs = FaultScanner(sigmaPhi,sigmaTheta)
    fl,fp,ft = fs.scan(minPhi,maxPhi,minTheta,maxTheta,p2,p3,gx)
    print "fl min =",min(fl)," max =",max(fl)
    print "fp min =",min(fp)," max =",max(fp)
    print "ft min =",min(ft)," max =",max(ft)
    writeImage(flfile,fl)
    writeImage(fpfile,fp)
    writeImage(ftfile,ft)
  else:
    fl = readImage3D(flfile)
  plot3(gx,fl,cmin=0.25,cmax=1.0,cmap=jetRamp(1.0),
      clab="Fault likelihood",png="fl")

def goFaultSkins():
  print "goSkin ..."
  lowerLikelihood = 0.5
  upperLikelihood = 0.8
  minSkinSize = 2000
  gx = readImage3D(gxfile)
  fl = readImage3D(flfile)
  fp = readImage3D(fpfile)
  ft = readImage3D(ftfile)
  fs = FaultSkinner()
  fs.setGrowLikelihoods(lowerLikelihood,upperLikelihood)
  fs.setMinSkinSize(minSkinSize)
  cells = fs.findCells([fl,fp,ft])
  skins = fs.findSkins(cells)
  sks = []
  for skin in skins:
    skin.smoothCellNormals(4)
    if(skin.getX1max()>80):
      sks.append(skin)
  print "total number of cells =",len(cells)
  print "total number of skins =",len(skins)
  print "number of cells in skins =",FaultSkin.countCells(skins)
  plot3(gx,skinx=sks,au=80,ev=55,v3=0.05,png="skind")

def gain(x):
  g = mul(x,x) 
  ref = RecursiveExponentialFilter(10.0)
  ref.apply1(g,g)
  y = zerofloat(n1,n2,n3)
  div(x,sqrt(g),y)
  return y

def smooth(sig,u):
  v = copy(u)
  rgf = RecursiveGaussianFilterP(sig)
  rgf.apply0(u,v)
  return v

def smooth2(sig1,sig2,u):
  v = copy(u)
  rgf1 = RecursiveGaussianFilterP(sig1)
  rgf2 = RecursiveGaussianFilterP(sig2)
  rgf1.apply0X(u,v)
  rgf2.applyX0(v,v)
  return v


def normalize(e):
  emin = min(e)
  emax = max(e)
  return mul(sub(e,emin),1.0/(emax-emin))

#############################################################################
# graphics

def jetFill(alpha):
  return ColorMap.setAlpha(ColorMap.JET,alpha)
def jetFillExceptMin(alpha):
  a = fillfloat(alpha,256)
  a[0] = 0.0
  return ColorMap.setAlpha(ColorMap.JET,a)
def jetRamp(alpha):
  return ColorMap.setAlpha(ColorMap.JET,rampfloat(0.0,alpha/256,256))
def bwrFill(alpha):
  return ColorMap.setAlpha(ColorMap.BLUE_WHITE_RED,alpha)
def bwrNotch(alpha):
  a = zerofloat(256)
  for i in range(len(a)):
    if i<128:
      a[i] = alpha*(128.0-i)/128.0
    else:
      a[i] = alpha*(i-127.0)/128.0
  return ColorMap.setAlpha(ColorMap.BLUE_WHITE_RED,a)
def hueFill(alpha):
  return ColorMap.getHue(0.0,1.0,alpha)
def hueFillExceptMin(alpha):
  a = fillfloat(alpha,256)
  a[0] = 0.0
  return ColorMap.setAlpha(ColorMap.getHue(0.0,1.0),a)

def addColorBar(frame,clab=None,cint=None):
  cbar = ColorBar(clab)
  if cint:
    cbar.setInterval(cint)
  cbar.setFont(Font("Arial",Font.PLAIN,32)) # size by experimenting
  cbar.setWidthMinimum
  cbar.setBackground(Color.WHITE)
  frame.add(cbar,BorderLayout.EAST)
  return cbar

def convertDips(ft):
  return FaultScanner.convertDips(0.2,ft) # 5:1 vertical exaggeration

def plot3(f,g=None,fbs=None,cmin=-2,cmax=2,cmap=None,clab=None,cint=None,
          sx=None,sy=None,surf=None,tgs=None,fd=None,cells=None,
          skins=None,skinx=None,au=45,ev=36,v3=-0.05,smax=0,links=False,png=None):
  n3 = len(f)
  n2 = len(f[0])
  n1 = len(f[0][0])
  s1,s2,s3=Sampling(n1),Sampling(n2),Sampling(n3)
  d1,d2,d3 = s1.delta,s2.delta,s3.delta
  f1,f2,f3 = s1.first,s2.first,s3.first
  l1,l2,l3 = s1.last,s2.last,s3.last
  sf = SimpleFrame(AxesOrientation.XRIGHT_YOUT_ZDOWN)
  cbar = None
  if g==None:
    ipg = sf.addImagePanels(s1,s2,s3,f)
    if cmap!=None:
      ipg.setColorModel(cmap)
    if cmin!=None and cmax!=None:
      ipg.setClips(cmin,cmax)
    else:
      ipg.setClips(-2.0,2.0)
    if clab:
      cbar = addColorBar(sf,clab,cint)
      ipg.addColorMapListener(cbar)
  else:
    ipg = ImagePanelGroup2(s1,s2,s3,f,g)
    ipg.setClips1(-2,2)
    if cmin!=None and cmax!=None:
      ipg.setClips2(cmin,cmax)
    if cmap==None:
      cmap = jetFill(0.8)
    ipg.setColorModel2(cmap)
    if clab:
      cbar = addColorBar(sf,clab,cint)
      ipg.addColorMap2Listener(cbar)
    sf.world.addChild(ipg)
  if cbar:
    cbar.setWidthMinimum(120)
  if fbs:
    mc = MarchingCubes(s1,s2,s3,fbs)
    ct = mc.getContour(0.0)
    tg = TriangleGroup(ct.i,ct.x,ct.u)
    states = StateSet()
    cs = ColorState()
    cs.setColor(Color.MAGENTA)
    states.add(cs)
    lms = LightModelState()
    lms.setTwoSide(True)
    states.add(lms)
    ms = MaterialState()
    ms.setColorMaterial(GL_AMBIENT_AND_DIFFUSE)
    ms.setSpecular(Color.WHITE)
    ms.setShininess(100.0)
    states.add(ms)
    tg.setStates(states);
    sf.world.addChild(tg)
  if cells:
    ss = StateSet()
    lms = LightModelState()
    lms.setTwoSide(True)
    ss.add(lms)
    ms = MaterialState()
    ms.setSpecular(Color.GRAY)
    ms.setShininess(100.0)
    ms.setColorMaterial(GL_AMBIENT_AND_DIFFUSE)
    ms.setEmissiveBack(Color(0.0,0.0,0.5))
    ss.add(ms)
    cmap = ColorMap(0.0,1.0,ColorMap.JET)
    xyz,uvw,rgb = FaultCell.getXyzUvwRgbForLikelihood(0.7,cmap,cells,False)
    qg = QuadGroup(xyz,uvw,rgb)
    qg.setStates(ss)
    sf.world.addChild(qg)
  if tgs:
    for tg in tgs:
      sf.world.addChild(tg)
  if surf:
    tg = TriangleGroup(True,surf)
    sf.world.addChild(tg)
  if skinx:
    sg = Group()
    ss = StateSet()
    lms = LightModelState()
    #lms.setTwoSide(False)
    lms.setTwoSide(False)
    ss.add(lms)
    ms = MaterialState()
    ms.setSpecular(Color.GRAY)
    ms.setShininess(100.0)
    ms.setColorMaterial(GL_AMBIENT_AND_DIFFUSE)
    if not smax:
      ms.setEmissiveBack(Color(0.0,0.0,0.0))
    ss.add(ms)
    sg.setStates(ss)
    for skin in skinx:
      #cmap = ColorMap(0.0,1.0,ColorMap.JET)
      cmap = ColorMap(0.0,180,hueFill(1.0))
      #tg = skin.getTriMesh(cmap)
      #tg = skin.getTriMeshStrike(cmap)
      qg = skin.getQuadMeshStrike(cmap)
      sg.addChild(qg)
    sf.world.addChild(sg)
  if skins:
    sg = Group()
    ss = StateSet()
    lms = LightModelState()
    #lms.setTwoSide(True)
    lms.setTwoSide(False)
    ss.add(lms)
    ms = MaterialState()
    ms.setSpecular(Color.GRAY)
    ms.setShininess(100.0)
    ms.setColorMaterial(GL_AMBIENT_AND_DIFFUSE)
    if not smax:
      ms.setEmissiveBack(Color(0.0,0.0,0.5))
    ss.add(ms)
    sg.setStates(ss)
    size = 2.0
    if links:
      size = 0.5 
    for skin in skins:
      if smax>0.0: # show fault throws
        cmap = ColorMap(0.0,smax,ColorMap.JET)
        xyz,uvw,rgb = skin.getCellXyzUvwRgbForThrow(size,cmap,False)
      else: # show fault likelihood
        cmap = ColorMap(0.25,1.0,ColorMap.JET)
        xyz,uvw,rgb = skin.getCellXyzUvwRgbForLikelihood(size,cmap,False)
      qg = QuadGroup(xyz,uvw,rgb)
      qg.setStates(None)
      sg.addChild(qg)
      if links:
        xyz = skin.getCellLinksXyz()
        lg = LineGroup(xyz)
        sg.addChild(lg)
    sf.world.addChild(sg)
  ipg.setSlices(106,80,207)
  ipg.setSlices(258,16,32)
  #ipg.setSlices(115,25,167)
  if cbar:
    sf.setSize(987,720)
  else:
    sf.setSize(850,720)
  vc = sf.getViewCanvas()
  vc.setBackground(Color.WHITE)
  radius = 0.5*sqrt(n1*n1+n2*n2+n3*n3)
  ov = sf.getOrbitView()
  zscale = 0.3*max(n2*d2,n3*d3)/(n1*d1)
  ov.setAxesScale(1.0,1.0,zscale)
  ov.setScale(1.55)
  ov.setWorldSphere(BoundingSphere(BoundingBox(f3,f2,f1,l3,l2,l1)))
  ov.setTranslate(Vector3(0.0,0.06,v3))
  ov.setAzimuthAndElevation(au,ev)
  sf.setVisible(True)
  if png and pngDir:
    sf.paintToFile(pngDir+png+".png")
    if cbar:
      cbar.paintToPng(720,1,pngDir+png+"cbar.png")
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