package br.com.resolveit.enums;

public enum StatusChamado {
    ABERTO,
    EM_ANDAMENTO,
    CONCLUIDO,
    FECHADO;

    public boolean podeMudarPara(StatusChamado novoStatus) {
        if(this == novoStatus) {
            return true;
        }

        return switch (this) {
            case ABERTO -> novoStatus == EM_ANDAMENTO;
            case EM_ANDAMENTO -> novoStatus == CONCLUIDO;
            case CONCLUIDO -> novoStatus == FECHADO;
            case FECHADO -> false;
        };
    }
}
