package br.com.clubedojava.webstore.service.impl;

import br.com.clubedojava.webstore.dto.ChatbotResponseDTO;
import br.com.clubedojava.webstore.service.ChatbotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@Slf4j
public class ChatbotServiceImpl implements ChatbotService {

    // Mapa de intenções e padrões
    private final Map<String, List<Pattern>> intentPatterns = new ConcurrentHashMap<>();

    // Mapa de respostas para cada intenção
    private final Map<String, List<String>> intentResponses = new ConcurrentHashMap<>();

    // Limiar de confiança para identificação de intenções
    @Value("${chatbot.confidence.threshold:0.6}")
    private float confidenceThreshold;

    // Cache de sessões de usuários para contexto
    private final Map<String, String> userSessionContext = new ConcurrentHashMap<>();

    // Construtor que inicializa os padrões e respostas
    public ChatbotServiceImpl() {
        initializeIntents();
    }

    @Override
    @Suspendable
    public CompletableFuture<ChatbotResponseDTO> processMessage(String message) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Identificação de intenção
                Map.Entry<String, Float> intentResult = identifyIntent(message);
                String intent = intentResult.getKey();
                Float confidence = intentResult.getValue();

                // Se a confiança for muito baixa, usa a intenção padrão
                if (confidence < confidenceThreshold) {
                    intent = "default";
                }

                // Obtém uma resposta da intenção identificada
                String response = getResponseForIntent(intent);

                return ChatbotResponseDTO.builder()
                        .answer(response)
                        .intent(intent)
                        .confidence(confidence)
                        .build();
            } catch (Exception e) {
                log.error("Erro ao processar mensagem do chatbot", e);
                return ChatbotResponseDTO.builder()
                        .answer("Desculpe, estou com problemas técnicos. Pode tentar novamente?")
                        .intent("error")
                        .confidence(0.0f)
                        .build();
            }
        });
    }

    @Override
    @Suspendable
    public CompletableFuture<String[]> getAvailableIntents() {
        return CompletableFuture.supplyAsync(() ->
                intentPatterns.keySet().toArray(new String[0])
        );
    }

    // Identifica a intenção baseado no texto da mensagem
    private Map.Entry<String, Float> identifyIntent(String message) {
        String normalizedMessage = message.toLowerCase().trim();
        Map<String, Float> scores = new HashMap<>();

        // Verifica cada intenção
        for (Map.Entry<String, List<Pattern>> entry : intentPatterns.entrySet()) {
            String intent = entry.getKey();
            List<Pattern> patterns = entry.getValue();

            // Calcula pontuação baseada em padrões correspondentes
            float score = 0.0f;
            for (Pattern pattern : patterns) {
                if (pattern.matcher(normalizedMessage).find()) {
                    score += 0.5f; // Cada padrão contribui com 0.5
                }
            }

            // Se encontrou alguma correspondência
            if (score > 0) {
                // Normaliza a pontuação (max 1.0)
                scores.put(intent, Math.min(score, 1.0f));
            }
        }

        // Se nenhuma intenção identificada, retorna default
        if (scores.isEmpty()) {
            return new AbstractMap.SimpleEntry<>("default", 0.0f);
        }

        // Retorna a intenção com maior pontuação
        return scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .orElse(new AbstractMap.SimpleEntry<>("default", 0.0f));
    }

    // Retorna uma resposta aleatória para a intenção
    private String getResponseForIntent(String intent) {
        List<String> responses = intentResponses.getOrDefault(intent, Collections.singletonList(
                "Desculpe, não entendi o que você disse. Pode reformular sua pergunta?"));

        int randomIndex = new Random().nextInt(responses.size());
        return responses.get(randomIndex);
    }

    // Inicializa as intenções, padrões e respostas
    private void initializeIntents() {
        // Saudação
        intentPatterns.put("greeting", Arrays.asList(
                Pattern.compile("\\b(oi|olá|hey|e aí|ola|bom dia|boa tarde|boa noite)\\b"),
                Pattern.compile("\\b(tudo bem|como vai|como está)\\b")
        ));
        intentResponses.put("greeting", Arrays.asList(
                "Olá! Eu sou o Duke, mascote do Java e assistente do Clube do Java! Como posso ajudar?",
                "E aí, tudo certo? Eu sou o Duke! Em que posso ser útil hoje?",
                "Olá! Duke ao seu dispor. Precisa de ajuda com algo?"
        ));

        intentPatterns.put("products", Arrays.asList(
                Pattern.compile("\\b(produto|produtos|camiseta|camisetas|caneca|canecas|adesivo|adesivos|jaqueta|jaquetas)\\b"),
                Pattern.compile("\\b(comprar|vender|oferta|promoção|desconto|preço|valor)\\b")
        ));
        intentResponses.put("products", Arrays.asList(
                "Temos camisetas, canecas, adesivos e jaquetas com temas de Desenvolvimento. O que você procura?",
                "Nossos produtos incluem mercadorias oficiais do Java, como camisetas oversized com estampas de codificação e canecas personalizadas!",
                "O Clube do Java oferece mercadorias de alta qualidade para desenvolvedores . Quer ver alguma categoria específica?"
        ));

        intentPatterns.put("payment", Arrays.asList(
                Pattern.compile("\\b(pagamento|pagar|pago|pix|boleto|cartão|cartao|débito|debito|crédito|credito)\\b"),
                Pattern.compile("\\b(forma|formas|método|metodo|condições|condicoes)\\b de \\b(pagamento|pagar)\\b")
        ));
        intentResponses.put("payment", Arrays.asList(
                "Aceitamos PIX, boleto bancário e cartões de crédito em até 12x! Qual método você prefere?",
                "Nossas formas de pagamento incluem PIX (com 5% de desconto), boleto e cartão de crédito em várias parcelas.",
                "Você pode pagar via PIX (processamento imediato), boleto (processamento em até 3 dias úteis) ou cartão de crédito!"
        ));

        intentPatterns.put("shipping", Arrays.asList(
                Pattern.compile("\\b(entrega|entregar|envio|enviar|despacho|frete|remessa|prazo|receber)\\b"),
                Pattern.compile("\\b(correios|transportadora|jadlog|braspress)\\b"),
                Pattern.compile("\\b(quando|tempo|dias|prazo)\\b.{0,30}\\b(chegar|chega|entrega)\\b")
        ));
        intentResponses.put("shipping", Arrays.asList(
                "Fazemos entregas por Correios, Jadlog e Braspress para todo o Brasil. O prazo varia de 2 a 15 dias úteis, dependendo da sua localização.",
                "Nosso frete é calculado com base no seu CEP. Na página de checkout você pode verificar todas as opções de entrega disponíveis para sua região!",
                "Trabalhamos com as principais transportadoras do Brasil. Assim que seu pedido for enviado, você receberá um código de rastreamento por email!"
        ));

        intentPatterns.put("tracking", Arrays.asList(
                Pattern.compile("\\b(rastreio|rastrear|rastreamento)\\b"),
                Pattern.compile("\\b(status|andamento)\\b.{0,20}\\b(pedido|compra|encomenda)\\b"),
                Pattern.compile("\\b(onde|como)\\b.{0,20}\\b(está|acompanhar)\\b.{0,20}\\b(pedido|encomenda)\\b")
        ));
        intentResponses.put("tracking", Arrays.asList(
                "Para rastrear seu pedido, acesse a seção 'Meus Pedidos' no seu perfil e clique no número de rastreio do pedido específico.",
                "Você pode acompanhar seu pedido na área 'Meus Pedidos' do seu perfil ou diretamente no site da transportadora usando o código de rastreio que enviamos por email.",
                "O rastreamento do seu pedido está disponível no seu perfil em 'Meus Pedidos'. Se tiver qualquer problema, nossa equipe de suporte pode ajudar!"
        ));

        // Trocas e devoluções
        intentPatterns.put("return", Arrays.asList(
                Pattern.compile("\\b(troca|trocar|devolução|devolver|reembolso|reembolsar)\\b"),
                Pattern.compile("\\b(prazo|política|politica)\\b.{0,20}\\b(troca|devolução|devoluçoes)\\b"),
                Pattern.compile("\\b(produto|camiseta|caneca)\\b.{0,30}\\b(defeito|errado|danificado|estragado)\\b")
        ));
        intentResponses.put("return", Arrays.asList(
                "Nossa política permite trocas e devoluções em até 7 dias após o recebimento. Basta entrar em contato com nosso suporte com fotos do produto e informações do pedido.",
                "Se você recebeu um produto com defeito ou errado, pode solicitar a troca em até 7 dias. Nós cobrimos os custos de envio da devolução!",
                "Para devoluções, acesse a seção 'Meus Pedidos', selecione o pedido específico e clique em 'Solicitar devolução'. Nossa equipe analisará seu caso em até 48h."
        ));

        // Tamanhos e medidas
        intentPatterns.put("sizes", Arrays.asList(
                Pattern.compile("\\b(tamanho|tamanhos|medida|medidas|dimensão|dimensões)\\b"),
                Pattern.compile("\\b(pequeno|médio|medio|grande|p|m|g|gg|xg|xxg)\\b"),
                Pattern.compile("\\b(tabela|guia)\\b.{0,20}\\b(tamanho|medida)\\b")
        ));
        intentResponses.put("sizes", Arrays.asList(
                "Temos tamanhos do P ao XXG para camisetas e moletons. Na página de cada produto você encontra uma tabela detalhada de medidas!",
                "Nossas camisetas seguem o estilo oversized, então ficam um pouco mais largas que o normal. Recomendamos conferir as medidas exatas na página do produto.",
                "Para cada produto, oferecemos uma tabela de medidas com largura, comprimento e outras dimensões relevantes. Assim fica mais fácil acertar seu tamanho ideal!"
        ));

        // Estoque
        intentPatterns.put("stock", Arrays.asList(
                Pattern.compile("\\b(estoque|disponível|disponivel|esgotado|acabou)\\b"),
                Pattern.compile("\\b(quando|vai|tempo)\\b.{0,20}\\b(voltar|reposição|reestoque)\\b"),
                Pattern.compile("\\b(tem|existe|há|ha|disponível)\\b.{0,30}\\b(tamanho|cor|modelo)\\b")
        ));
        intentResponses.put("stock", Arrays.asList(
                "A disponibilidade de estoque é mostrada na página de cada produto. Se algo estiver esgotado, você pode cadastrar seu email para ser notificado quando voltar!",
                "Atualizamos nosso estoque regularmente, geralmente a cada 15 dias. Se um item específico estiver esgotado, tente novamente em breve!",
                "Para verificar o estoque de um produto específico, basta acessar sua página e selecionar o tamanho/cor desejado. O sistema mostrará a disponibilidade atual."
        ));

        // Sobre Java e programação
        intentPatterns.put("java", Arrays.asList(
                Pattern.compile("\\b(java|programação|programaçao|programar|código|codigo|developer|dev)\\b"),
                Pattern.compile("\\b(o que é|aprender|estudar|curso|tutorial)\\b.{0,20}\\b(java|programação)\\b"),
                Pattern.compile("\\b(duke|mascote)\\b.{0,20}\\b(java|sun|oracle)\\b")
        ));
        intentResponses.put("java", Arrays.asList(
                "Java é uma linguagem de programação e plataforma computacional lançada pela primeira vez pela Sun Microsystems em 1995. E eu, Duke, sou o mascote oficial do Java!",
                "Eu sou o Duke, o mascote oficial do Java! Fui criado em 1992 pelo designer Joe Palrang. Represento o espírito divertido e dinâmico do Java!",
                "Java é uma das linguagens de programação mais populares do mundo! É versátil, robusta e roda em bilhões de dispositivos. E eu sou o Duke, seu mascote oficial desde 1992!"
        ));

        // Agradecimento
        intentPatterns.put("thanks", Arrays.asList(
                Pattern.compile("\\b(obrigado|obrigada|valeu|grato|grata|agradecido|agradeço|agradece)\\b"),
                Pattern.compile("\\b(ajudou|ajuda|útil|util)\\b")
        ));
        intentResponses.put("thanks", Arrays.asList(
                "De nada! Estou sempre à disposição para ajudar! 😊",
                "Fico feliz em ter ajudado! Se precisar de mais alguma coisa, é só chamar!",
                "Disponha! É um prazer poder ajudar! Se tiver mais dúvidas, estou aqui!"
        ));

        // Despedida
        intentPatterns.put("goodbye", Arrays.asList(
                Pattern.compile("\\b(tchau|adeus|até|ate|logo|mais|fim|encerrar|sair)\\b"),
                Pattern.compile("\\b(conversa|finalizar|terminar)\\b")
        ));
        intentResponses.put("goodbye", Arrays.asList(
                "Até logo! Foi um prazer te ajudar! Volte sempre ao Clube do Java! 👋",
                "Tchau! Se precisar de mais ajuda, é só me chamar novamente! 👋",
                "Até a próxima! Espero que tenha uma excelente experiência no Clube do Java! 👋"
        ));

        // Piadas
        intentPatterns.put("joke", Arrays.asList(
                Pattern.compile("\\b(piada|piadas|engraçado|engraçada|engraçar|humor|divertido|divertida)\\b"),
                Pattern.compile("\\b(conte|contar|falar|dizer)\\b.{0,20}\\b(piada|algo engraçado)\\b"),
                Pattern.compile("\\b(me|faz|faça|fazer)\\b.{0,20}\\b(rir|sorrir|gargalhar)\\b")
        ));
        intentResponses.put("joke", Arrays.asList(
                "Por que o programador Java foi ao médico? Porque ele estava tendo NullPointerException ao tentar se lembrar do nome da namorada! 😂",
                "O que um programador Java disse para o outro? Você está no meu escopo privado! 😂",
                "Quantos programadores são necessários para trocar uma lâmpada? Nenhum, é um problema de hardware! 😂",
                "Por que eu, o Duke, nunca fico parado? Porque estou sempre em uma Virtual Thread! 😂"
        ));

        // Contato e Suporte
        intentPatterns.put("contact", Arrays.asList(
                Pattern.compile("\\b(contato|falar|email|telefone|whatsapp|chat|atendimento|sac|suporte)\\b"),
                Pattern.compile("\\b(humano|pessoa|atendente|vendedor|gerente)\\b"),
                Pattern.compile("\\b(dúvida|duvida|problema|reclamação|reclamacao|sugestão|sugestao)\\b")
        ));
        intentResponses.put("contact", Arrays.asList(
                "Para falar com nossa equipe de suporte, envie um email para contato@clubedojava.com.br ou chame no WhatsApp (11) 99999-9999, disponível em horário comercial.",
                "Você pode entrar em contato com nosso time pelo email contato@clubedojava.com.br ou WhatsApp (11) 99999-9999. Respondemos em até 24 horas úteis!",
                "Para atendimento humano, envie um email para contato@clubedojava.com.br com seus dados e dúvida, ou chame no WhatsApp (11) 99999-9999, disponível de segunda a sexta, das 9h às 18h."
        ));

        // Default (Fallback)
        intentPatterns.put("default", Arrays.asList(
                Pattern.compile(".*")
        ));
        intentResponses.put("default", Arrays.asList(
                "Desculpe, não entendi o que você quis dizer. Pode reformular ou escolher um dos tópicos: produtos, pagamentos, entregas ou trocas?",
                "Hmm, não tenho certeza do que você está perguntando. Posso ajudar com informações sobre produtos, pagamentos, entregas ou suporte. O que você precisa?",
                "Não consegui entender sua pergunta. Posso ajudar com nossos produtos, formas de pagamento, prazos de entrega ou política de trocas. O que deseja saber?",
                "Como mascote do Java, às vezes tenho bugs de compreensão! 😅 Poderia reformular sua pergunta? Posso falar sobre produtos, pagamentos, entregas ou trocas."
        ));
    }
}