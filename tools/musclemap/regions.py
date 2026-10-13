# Region polygons authored in the 4x gridded crops, then converted to chart pixel space (422x316).
# FRONT: crop starts at x=0.  BACK: crop starts at x=201.  Mirrored about each figure's midline.
FRONT_MID = 920.0    # display x of midline in front crop (2*115*4 / 2 -> mirror x' = 920 - x)
BACK_MID = 1010.0    # display x of midline in back crop  (mirror x' = 1010 - x)
BACK_X0 = 201.0

def mirror(poly, mid):
    return [(mid - x, y) for x, y in reversed(poly)]

FRONT = {
 "PECTORALS": [[(340,240),(458,248),(458,305),(402,338),(344,322)]],
 "ANTERIOR_DELTOID": [[(262,232),(330,236),(342,300),(300,320),(258,300),(246,268)]],
 "BICEPS": [[(262,340),(330,335),(322,420),(300,470),(272,470),(258,400)]],
 "FOREARMS": [[(214,480),(275,480),(265,560),(228,560),(212,520)]],
 "ABDOMINALS": [[(405,340),(458,345),(458,465),(425,470),(408,420),(402,370)]],
 "OBLIQUES": [[(360,345),(402,340),(408,420),(425,470),(395,500),(365,460),(352,400)]],
 "QUADRICEPS": [[(330,600),(395,600),(410,650),(405,740),(380,790),(340,800),(320,740),(315,660)]],
 "ADDUCTORS": [[(408,600),(440,600),(440,740),(420,760),(410,650)]],
 "TIBIALIS_ANTERIOR": [[(335,800),(380,800),(395,870),(390,930),(360,930),(340,870)]],
}
BACK = {
 "TRAPEZIUS": [[(505,175),(470,175),(440,220),(470,265),(505,280)]],
 "POSTERIOR_DELTOID": [[(330,230),(370,235),(370,300),(340,320),(305,300),(300,260)]],
 "LATISSIMUS_DORSI": [[(370,320),(420,330),(430,410),(425,480),(400,490),(378,410)]],
 "TRICEPS": [[(262,330),(300,340),(305,420),(285,465),(265,440),(258,390)]],
 "LOWER_BACK": [[(470,380),(505,390),(505,500),(478,480),(465,430)]],
 "GLUTEAL_GROUP": [[(405,570),(460,575),(462,650),(430,660),(400,630)]],
 "HAMSTRINGS": [[(375,650),(425,650),(430,740),(415,790),(390,790),(372,720)]],
 "CALVES": [[(380,820),(425,820),(435,900),(420,930),(390,925),(378,870)]],
}

def to_chart(polys, mid, x0, scale=4.0):
    out = {}
    for key, plist in polys.items():
        res = []
        for p in plist:
            full = p + mirror(p, mid)  # half + mirrored half, as one polygon per side
            for side in (p, mirror(p, mid)):
                res.append([(round(x0 + x/scale, 2), round(y/scale, 2)) for x, y in side])
        out[key] = res
    return out

FRONT_PX = to_chart(FRONT, FRONT_MID, 0.0)
BACK_PX = to_chart(BACK, BACK_MID, BACK_X0)
W, H = 422, 316
