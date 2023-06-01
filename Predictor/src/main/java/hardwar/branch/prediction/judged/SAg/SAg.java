package hardwar.branch.prediction.judged.SAg;



import hardwar.branch.prediction.shared.*;
import hardwar.branch.prediction.shared.devices.*;

import java.util.Arrays;

public class SAg implements BranchPredictor {
    private final int branchInstructionSize;
    private final int KSize;
    private final ShiftRegister SC; // saturating counter register
    private final RegisterBank PSBHR; // per set branch history register
    private final Cache<Bit[], Bit[]> PHT; // page history table

    public SAg() {
        this(4, 2, 8, 4);
    }

    public SAg(int BHRSize, int SCSize, int branchInstructionSize, int KSize) {
        // TODO: complete the constructor
        this.branchInstructionSize = branchInstructionSize;
        this.KSize = KSize;

        // Initialize the PABHR with the given bhr and Ksize
        PSBHR = new RegisterBank(this.KSize, BHRSize);

        // Initialize the PHT with a size of 2^size and each entry having a saturating counter of size "SCSize"
        PHT = new PageHistoryTable((int) Math.pow(2, BHRSize), 2);

        // Initialize the SC register
        SC = new SIPORegister("sc-forus", SCSize, null);
    }

    @Override
    public BranchResult predict(BranchInstruction instruction) {
        // TODO: complete Task 1
        Bit[] address_PSBHR = hash(instruction.getInstructionAddress());
        ShiftRegister shiftRegister_content_PSBHR = this.PSBHR.read(address_PSBHR);
        Bit[] history = shiftRegister_content_PSBHR.read();
        Bit[] prediction = this.PHT.setDefault(history, getDefaultBlock());
        return BranchResult.of(prediction[0].getValue());
    }

    @Override
    public void update(BranchInstruction branchInstruction, BranchResult actual) {
        // TODO: complete Task 2
        Bit[] address_PSBHR = hash(branchInstruction.getInstructionAddress());
        ShiftRegister shiftRegister_content_PSBHR = this.PSBHR.read(address_PSBHR);
        Bit[] history = shiftRegister_content_PSBHR.read();
        Bit[] prediction = this.PHT.setDefault(history, getDefaultBlock());

        this.PHT.put(history, CombinationalLogic.count(prediction, BranchResult.isTaken(actual), CountMode.SATURATING));
        shiftRegister_content_PSBHR.insert(Bit.of(BranchResult.isTaken(actual)));

//
//        this.PAPHT.put(address, CombinationalLogic.count(prediction, BranchResult.isTaken(actual), CountMode.SATURATING));
//        this.BHR.insert(Bit.of(BranchResult.isTaken(actual)));
    }

    private Bit[] getRBAddressLine(Bit[] branchAddress) {
        // hash the branch address
        return hash(branchAddress);
    }

    /**
     * hash N bits to a K bit value
     *
     * @param bits program counter
     * @return hash value of fist M bits of `bits` in K bits
     */
    private Bit[] hash(Bit[] bits) {
        Bit[] hash = new Bit[KSize];

        // XOR the first M bits of the PC to produce the hash
        for (int i = 0; i < branchInstructionSize; i++) {
            int j = i % KSize;
            if (hash[j] == null) {
                hash[j] = bits[i];
            } else {
                Bit xorProduce = hash[j].getValue() ^ bits[i].getValue() ? Bit.ONE : Bit.ZERO;
                hash[j] = xorProduce;

            }
        }
        return hash;
    }

    /**
     * @return a zero series of bits as default value of cache block
     */
    private Bit[] getDefaultBlock() {
        Bit[] defaultBlock = new Bit[SC.getLength()];
        Arrays.fill(defaultBlock, Bit.ZERO);
        return defaultBlock;
    }

    @Override
    public String monitor() {
        return null;
    }
}
