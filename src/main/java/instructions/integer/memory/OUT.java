package instructions.integer.memory;

import cpu.CPU;
import cpu.interrupts.exceptions.InterruptException;
import instructions.Instruction;
import operands.OperandsR;

public class OUT extends Instruction<OperandsR> {
    public OUT() {
        super(85, "out");
    }

    @Override
    public void execute(CPU cpu, OperandsR operands) throws InterruptException {
        cpu.dataBus.isolatedOut(cpu.port, cpu.intRegs.get(operands.register).getValue());
    }
}
