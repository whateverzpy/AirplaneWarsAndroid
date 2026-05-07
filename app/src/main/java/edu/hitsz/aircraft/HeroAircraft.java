package edu.hitsz.aircraft;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.strategy.DirectShoot;
import edu.hitsz.strategy.ShootStrategy;

import java.util.List;

/**
 * 英雄飞机，游戏玩家操控
 *
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    // 攻击方式

    /**
     * 单例模式实例
     */
    private static volatile HeroAircraft instance = null;
    /**
     * 子弹一次发射数量
     */
    private int shootNum = 1;
    /**
     * 子弹伤害
     */
    private int power = 30;
    /**
     * 子弹射击方向 (向上发射：-1，向下发射：1)
     */
    private int direction = -1;
    /**
     * 火力道具计时器线程
     */
    private volatile Thread powerUpTimer = null;

    /**
     * 构造函数私有化
     *
     * @param locationX 英雄机位置x坐标
     * @param locationY 英雄机位置y坐标
     * @param speedX    英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param speedY    英雄机射出的子弹的基准速度（英雄机无特定速度）
     * @param hp        初始生命值
     */
    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        this.shootNum = 1;
        this.power = 30;
        this.direction = -1;
        this.shootStrategy = new DirectShoot();
        this.shootCycle = 100;
    }

    /**
     * 获取英雄机单例（双重检查锁定）
     *
     * @return 英雄机实例
     */
    public static HeroAircraft getInstance() {
        if (instance == null) {
            synchronized (HeroAircraft.class) {
                if (instance == null) {
                    instance = new HeroAircraft(
                            edu.hitsz.application.Main.WINDOW_WIDTH / 2,
                            edu.hitsz.application.Main.WINDOW_HEIGHT
                                    - edu.hitsz.application.ImageManager.HERO_IMAGE.getHeight(),
                            0, 0, 100);
                }
            }
        }
        return instance;
    }

    /**
     * 重置单例英雄机，确保每一局都从完整生命值和默认射击状态开始。
     *
     * @param locationX 初始 x 坐标
     * @param locationY 初始 y 坐标
     */
    public void resetForNewGame(int locationX, int locationY) {
        if (powerUpTimer != null && powerUpTimer.isAlive()) {
            powerUpTimer.interrupt();
        }
        powerUpTimer = null;

        setLocation(locationX, locationY);
        this.speedX = 0;
        this.speedY = 0;
        this.hp = this.maxHp;
        this.isValid = true;
        this.power = 30;
        this.direction = -1;
        this.shootTimer = 0;
        revertToDefaultShoot();
    }

    public int getShootNum() {
        return shootNum;
    }

    public void setShootNum(int shootNum) {
        this.shootNum = shootNum;
    }

    /**
     * 激活火力道具，设置新的射击策略并启动一个计时器，在持续时间结束后恢复默认。
     * 如果已有火力道具效果正在生效，会先中断旧的计时器。
     *
     * @param strategy 新的射击策略
     * @param shootNum 新的子弹数量
     * @param duration 效果持续时间 (ms)
     */
    public void activatePowerUp(ShootStrategy strategy, int shootNum, int duration) {
        // 如果已有火力道具计时器在运行，则中断它
        if (powerUpTimer != null && powerUpTimer.isAlive()) {
            powerUpTimer.interrupt();
        }

        // 设置新的射击策略和子弹数量
        this.setShootStrategy(strategy);
        this.setShootNum(shootNum);

        // 创建并启动新的计时器线程
        powerUpTimer = new Thread(() -> {
            try {
                Thread.sleep(duration);
                // 持续时间到，恢复默认射击模式
                System.out.println("Power-up expired. Reverting to DirectShoot.");
                revertToDefaultShoot();
            } catch (InterruptedException e) {
                // 计时器被中断（例如，因为获得了新的火力道具），提前结束
                System.out.println("Power-up timer interrupted.");
            }
        });
        powerUpTimer.start();
    }

    /**
     * 恢复为默认的直射模式
     */
    public void revertToDefaultShoot() {
        this.setShootStrategy(new DirectShoot());
        this.setShootNum(1);
    }

    @Override
    public void forward() {
        // 英雄机由鼠标控制，不通过forward函数移动
    }

    /**
     * 英雄机射击，具体实现委托给射击策略
     *
     * @return 射出的子弹 List
     */
    @Override
    public List<BaseBullet> shoot() {
        return shootStrategy.shoot(
                this.getLocationX(),
                this.getLocationY() + direction * 2, // 子弹从机头射出
                0, // 英雄机子弹不继承飞机速度
                this.getSpeedY(),
                this.direction,
                this.shootNum,
                this.power);
    }

}
