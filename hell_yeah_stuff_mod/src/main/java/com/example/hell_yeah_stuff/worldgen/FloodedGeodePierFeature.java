package com.example.hell_yeah_stuff.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
public class FloodedGeodePierFeature extends Feature<NoneFeatureConfiguration> {
    public FloodedGeodePierFeature(){ super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx){
        WorldGenLevel level=ctx.level(); BlockPos origin=ctx.origin(); RandomSource rnd=ctx.random();
        // pier is part of geode structure — check disabled per user request
        // if(!isGeodeArea(level,origin)) return false;
        BlockPos floor=findFloor(level,origin);
        if(floor==null) return false;
        int waterY=findWaterSurface(level,floor);
        boolean hasWater=waterY>floor.getY()+1;
        Direction dir=Direction.Plane.HORIZONTAL.getRandomDirection(rnd);
        int len=5+rnd.nextInt(5);
        int width=2;
        // Pier deck: along dir
        for(int i=0;i<len;i++){
            for(int w=-1;w<=0;w++){
                BlockPos p=floor.offset(dir.getStepX()*i + (dir.getAxis()==Direction.Axis.X?0:w),0, dir.getStepZ()*i + (dir.getAxis()==Direction.Axis.Z?0:w));
                BlockState deck=Blocks.OAK_PLANKS.defaultBlockState();
                // deck one above floor if flooded, else on floor
                BlockPos deckPos=hasWater? new BlockPos(p.getX(), waterY, p.getZ()): p.above();
                if(level.getBlockState(deckPos).canBeReplaced()) level.setBlock(deckPos,deck,2);
                // fence posts / supports
                if(i%3==0){
                    BlockPos f1=deckPos.relative(dir.getClockWise());
                    BlockPos f2=deckPos.relative(dir.getCounterClockWise());
                    tryFence(level,f1,deckPos.getY());
                    tryFence(level,f2,deckPos.getY());
                }
                // supports down to floor under water
                if(hasWater){
                    for(int y=deckPos.getY()-1;y>floor.getY();y--){
                        BlockPos sup=new BlockPos(p.getX(),y,p.getZ());
                        BlockState cur=level.getBlockState(sup);
                        if(cur.isAir()||cur.getFluidState().is(Fluids.WATER)) level.setBlock(sup,Blocks.OAK_LOG.defaultBlockState(),2);
                    }
                }
            }
        }
        // small excavation pit near geode wall + chest/barrel
        BlockPos pitCenter=floor.relative(dir,len-1);
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++){
            BlockPos pp=new BlockPos(pitCenter.getX()+dx,pitCenter.getY(),pitCenter.getZ()+dz);
            // dig 1 down if solid to make shallow pit
            BlockState below=level.getBlockState(pp.below());
            if(below.isFaceSturdy(level,pp.below(),Direction.UP)){
                level.setBlock(pp,Blocks.AIR.defaultBlockState(),2);
            }
        }
        // lantern / small camp
        BlockPos lanternPos=floor.relative(dir,len-2).above(hasWater? waterY - floor.getY() +1 :1);
        if(level.getBlockState(lanternPos).canBeReplaced()) level.setBlock(lanternPos,Blocks.LANTERN.defaultBlockState(),2);
        BlockPos barrelPos=pitCenter.above(hasWater? waterY - floor.getY():1);
        if(level.getBlockState(barrelPos).canBeReplaced()){
            level.setBlock(barrelPos,Blocks.BARREL.defaultBlockState(),2);
            // loot will be assigned via loot table data if desired; for now leave empty
        }
        return true;
    }
    private boolean isGeodeArea(WorldGenLevel l, BlockPos o){
        int r=6;
        int amethyst=0, budding=0;
        for(int dx=-r;dx<=r;dx++) for(int dy=-r;dy<=r;dy++) for(int dz=-r;dz<=r;dz++){
            BlockState s=l.getBlockState(o.offset(dx,dy,dz));
            if(s.is(Blocks.AMETHYST_BLOCK)||s.is(Blocks.CALCITE)||s.is(Blocks.SMOOTH_BASALT)) amethyst++;
            if(s.is(Blocks.BUDDING_AMETHYST)) budding++;
        }
        return budding>0 || amethyst>12;
    }
    private BlockPos findFloor(WorldGenLevel l, BlockPos o){
        BlockPos p=o;
        for(int y=0;y<10;y++){
            BlockPos cur=p.below(y);
            BlockState s=l.getBlockState(cur);
            BlockState above=l.getBlockState(cur.above());
            if(s.isFaceSturdy(l,cur,Direction.UP) && (above.isAir()||above.getFluidState().is(Fluids.WATER))) return cur.above();
        }
        // fallback: scan down from origin until solid
        BlockPos q=o;
        for(int y=0;y<16;y++){ if(l.getBlockState(q.below(y)).isFaceSturdy(l,q.below(y),Direction.UP)) return q.below(y).above(); }
        return null;
    }
    private int findWaterSurface(WorldGenLevel l, BlockPos floor){
        BlockPos p=floor;
        for(int y=0;y<8;y++){
            BlockPos cur=p.above(y);
            if(l.getBlockState(cur).getFluidState().is(Fluids.WATER)){
                // go up to surface
                int yy=y;
                while(yy<12 && l.getBlockState(p.above(yy)).getFluidState().is(Fluids.WATER)) yy++;
                return p.getY()+yy-1;
            }
        }
        return floor.getY();
    }
    private void tryFence(WorldGenLevel l, BlockPos p,int deckY){
        BlockPos fp=new BlockPos(p.getX(),deckY,p.getZ());
        if(l.getBlockState(fp).canBeReplaced()) l.setBlock(fp,Blocks.OAK_FENCE.defaultBlockState(),2);
    }
}

