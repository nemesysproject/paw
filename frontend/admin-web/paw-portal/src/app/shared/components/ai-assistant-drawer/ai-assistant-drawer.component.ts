import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';
import {
  AiAssistantService,
  AiChatMessage,
  AiNavigationAction
} from '@core/services/ai-assistant.service';

interface UiChatMessage extends AiChatMessage {
  actions?: AiNavigationAction[];
  provider?: string;
}

@Component({
  selector: 'app-ai-assistant-drawer',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <!-- Botón flotante para abrir el Copiloto IA -->
    <button
      *ngIf="!isOpen()"
      class="ai-fab shadow-lg"
      (click)="toggleDrawer()"
      title="Abrir PAW Assistant (IA)"
    >
      <lucide-icon name="sparkles" [size]="20"></lucide-icon>
      <span>PAW IA</span>
    </button>

    <!-- Drawer lateral -->
    <div class="ai-drawer shadow-lg" *ngIf="isOpen()">
      <div class="ai-drawer-header">
        <div class="d-flex align-items-center gap-2">
          <div class="ai-badge-icon">
            <lucide-icon name="sparkles" [size]="18"></lucide-icon>
          </div>
          <div>
            <div class="fw-bold text-white lh-1">PAW Assistant</div>
            <small class="text-white-50">Consultas y navegación con IA</small>
          </div>
        </div>
        <button class="btn btn-sm btn-link text-white p-1" (click)="toggleDrawer()" title="Cerrar">
          <lucide-icon name="x" [size]="18"></lucide-icon>
        </button>
      </div>

      <!-- Sugerencias rápidas -->
      <div class="ai-quick-prompts" *ngIf="messages().length <= 1">
        <div class="small text-muted fw-semibold mb-2">Prueba preguntando:</div>
        <div class="d-flex flex-wrap gap-1">
          <button
            *ngFor="let prompt of quickPrompts"
            class="btn btn-sm btn-outline-primary rounded-pill quick-chip"
            (click)="askQuickPrompt(prompt)"
            [disabled]="isLoading()"
          >
            {{ prompt }}
          </button>
        </div>
      </div>

      <!-- Historial de mensajes -->
      <div class="ai-messages">
        <div
          *ngFor="let msg of messages()"
          class="ai-msg-wrapper"
          [class.user-msg]="msg.role === 'user'"
          [class.assistant-msg]="msg.role === 'assistant'"
        >
          <div class="ai-bubble">
            <div class="ai-bubble-text">{{ msg.content }}</div>

            <!-- Botones de navegación sugeridos por el copiloto -->
            <div class="d-flex flex-wrap gap-1 mt-2" *ngIf="msg.actions && msg.actions.length > 0">
              <button
                *ngFor="let act of msg.actions"
                class="btn btn-sm btn-light border action-pill d-inline-flex align-items-center gap-1"
                (click)="navigateTo(act.route)"
              >
                <span>{{ act.label }}</span>
                <lucide-icon name="arrow-up-right" [size]="13"></lucide-icon>
              </button>
            </div>

            <div *ngIf="msg.provider" class="provider-tag mt-1">
              {{ msg.provider === 'gemini' ? 'Gemini API' : 'Modo Local PoC' }}
            </div>
          </div>
        </div>

        <div *ngIf="isLoading()" class="ai-msg-wrapper assistant-msg">
          <div class="ai-bubble text-muted small">
            Consultando datos en vivo de PAW...
          </div>
        </div>
      </div>

      <!-- Input -->
      <form class="ai-input-bar" (ngSubmit)="send()">
        <input
          type="text"
          class="form-control form-control-sm"
          name="prompt"
          [(ngModel)]="draft"
          placeholder="Pregunta sobre mascotas, refugios, campañas..."
          [disabled]="isLoading()"
          autocomplete="off"
        />
        <button
          type="submit"
          class="btn btn-primary btn-sm px-3 d-flex align-items-center"
          [disabled]="isLoading() || !draft.trim()"
        >
          <lucide-icon name="send" [size]="15"></lucide-icon>
        </button>
      </form>
    </div>
  `,
  styles: [`
    .ai-fab {
      position: fixed;
      right: 24px;
      bottom: 24px;
      z-index: 1050;
      border: 1px solid rgba(255, 255, 255, 0.45);
      border-radius: 999px;
      padding: 12px 20px;
      background: rgba(255, 255, 255, 0.22);
      backdrop-filter: blur(22px) saturate(170%);
      -webkit-backdrop-filter: blur(22px) saturate(170%);
      box-shadow: 0 10px 28px rgba(10, 15, 36, 0.35), inset 0 1px 0 rgba(255, 255, 255, 0.55);
      color: #ffffff;
      font-weight: 700;
      font-size: 0.9rem;
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      transition: transform 0.2s ease, background 0.2s ease;
    }

    .ai-fab:hover {
      transform: translateY(-2px);
      background: rgba(255, 255, 255, 0.32);
    }

    .ai-drawer {
      position: fixed;
      right: 20px;
      bottom: 20px;
      width: 390px;
      max-width: calc(100vw - 32px);
      height: 540px;
      max-height: calc(100vh - 100px);
      background: rgba(20, 28, 60, 0.58);
      backdrop-filter: blur(28px) saturate(175%);
      -webkit-backdrop-filter: blur(28px) saturate(175%);
      border-radius: 24px;
      border: 1px solid rgba(255, 255, 255, 0.34);
      box-shadow: 0 18px 44px rgba(8, 12, 32, 0.5), inset 0 1px 0 rgba(255, 255, 255, 0.4);
      display: flex;
      flex-direction: column;
      overflow: hidden;
      z-index: 1060;
    }

    .ai-drawer-header {
      background: rgba(255, 255, 255, 0.12);
      border-bottom: 1px solid rgba(255, 255, 255, 0.2);
      padding: 14px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .ai-badge-icon {
      width: 34px;
      height: 34px;
      border-radius: 12px;
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.35);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .ai-quick-prompts {
      padding: 10px 14px;
      background: rgba(255, 255, 255, 0.06);
      border-bottom: 1px solid rgba(255, 255, 255, 0.14);
    }

    .quick-chip {
      font-size: 0.75rem;
      padding: 3px 10px;
    }

    .ai-messages {
      flex: 1;
      overflow-y: auto;
      padding: 14px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      background: transparent;
    }

    .ai-msg-wrapper {
      display: flex;
    }

    .user-msg {
      justify-content: flex-end;
    }

    .assistant-msg {
      justify-content: flex-start;
    }

    .ai-bubble {
      max-width: 86%;
      padding: 10px 13px;
      border-radius: 16px;
      font-size: 0.85rem;
      line-height: 1.4;
      white-space: pre-wrap;
    }

    .user-msg .ai-bubble {
      background: rgba(129, 140, 248, 0.42);
      border: 1px solid rgba(255, 255, 255, 0.38);
      color: #ffffff;
      border-bottom-right-radius: 4px;
    }

    .assistant-msg .ai-bubble {
      background: rgba(255, 255, 255, 0.16);
      color: #ffffff;
      border: 1px solid rgba(255, 255, 255, 0.26);
      border-bottom-left-radius: 4px;
    }

    .action-pill {
      font-size: 0.75rem;
      font-weight: 600;
      color: #ffffff !important;
      background: rgba(255, 255, 255, 0.18) !important;
      border: 1px solid rgba(255, 255, 255, 0.35) !important;
      border-radius: 999px;
      padding: 3px 10px;
    }

    .provider-tag {
      font-size: 0.65rem;
      color: rgba(255, 255, 255, 0.65);
      text-align: right;
    }

    .ai-input-bar {
      padding: 10px 12px;
      background: rgba(255, 255, 255, 0.1);
      border-top: 1px solid rgba(255, 255, 255, 0.2);
      display: flex;
      gap: 8px;
    }
  `]
})
export class AiAssistantDrawerComponent {
  private aiService = inject(AiAssistantService);
  private router = inject(Router);

  isOpen = signal(false);
  isLoading = signal(false);
  draft = '';

  quickPrompts = [
    'Resumen de mascotas en riesgo y perdidas',
    '¿Qué campañas activas tienen mejor CTR?',
    '¿Cuántos refugios y veterinarias tenemos?'
  ];

  messages = signal<UiChatMessage[]>([
    {
      role: 'assistant',
      content:
        '¡Hola! Soy PAW Assistant. Puedo consultar en vivo mascotas, refugios, veterinarias y campañas publicitarias, y llevarte directo a cada sección.'
    }
  ]);

  toggleDrawer(): void {
    this.isOpen.update((open) => !open);
  }

  askQuickPrompt(prompt: string): void {
    this.draft = prompt;
    this.send();
  }

  navigateTo(route: string): void {
    if (route.startsWith('/')) {
      this.router.navigateByUrl(route);
    }
  }

  send(): void {
    const text = this.draft.trim();
    if (!text || this.isLoading()) return;

    const history: AiChatMessage[] = this.messages()
      .slice(-8)
      .map((m) => ({ role: m.role, content: m.content }));

    this.messages.update((list) => [...list, { role: 'user', content: text }]);
    this.draft = '';
    this.isLoading.set(true);

    this.aiService.sendMessage(text, history).subscribe({
      next: (res) => {
        this.messages.update((list) => [
          ...list,
          {
            role: 'assistant',
            content: res.reply,
            actions: res.actions,
            provider: res.provider
          }
        ]);
        this.isLoading.set(false);
      },
      error: () => {
        this.messages.update((list) => [
          ...list,
          {
            role: 'assistant',
            content:
              'No pude conectarme con el servicio de IA. Verifica que el backend esté activo e intenta nuevamente.'
          }
        ]);
        this.isLoading.set(false);
      }
    });
  }
}
