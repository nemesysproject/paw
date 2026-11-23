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
      border: none;
      border-radius: 999px;
      padding: 12px 18px;
      background: linear-gradient(135deg, #4f46e5, #6366f1);
      color: #fff;
      font-weight: 700;
      font-size: 0.9rem;
      display: flex;
      align-items: center;
      gap: 8px;
      cursor: pointer;
      transition: transform 0.2s ease, box-shadow 0.2s ease;
    }

    .ai-fab:hover {
      transform: translateY(-2px);
    }

    .ai-drawer {
      position: fixed;
      right: 20px;
      bottom: 20px;
      width: 390px;
      max-width: calc(100vw - 32px);
      height: 540px;
      max-height: calc(100vh - 100px);
      background: #ffffff;
      border-radius: 16px;
      border: 1px solid #e2e8f0;
      display: flex;
      flex-direction: column;
      overflow: hidden;
      z-index: 1060;
    }

    .ai-drawer-header {
      background: linear-gradient(135deg, #1e293b, #4f46e5);
      padding: 14px 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .ai-badge-icon {
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.16);
      color: #fff;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .ai-quick-prompts {
      padding: 10px 14px;
      background: #f8fafc;
      border-bottom: 1px solid #e2e8f0;
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
      background: #f8fafc;
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
      padding: 10px 12px;
      border-radius: 14px;
      font-size: 0.85rem;
      line-height: 1.4;
      white-space: pre-wrap;
    }

    .user-msg .ai-bubble {
      background: #4f46e5;
      color: #fff;
      border-bottom-right-radius: 4px;
    }

    .assistant-msg .ai-bubble {
      background: #ffffff;
      color: #1e293b;
      border: 1px solid #e2e8f0;
      border-bottom-left-radius: 4px;
    }

    .action-pill {
      font-size: 0.75rem;
      font-weight: 600;
      color: #4f46e5;
      border-radius: 999px;
      padding: 3px 10px;
    }

    .provider-tag {
      font-size: 0.65rem;
      color: #94a3b8;
      text-align: right;
    }

    .ai-input-bar {
      padding: 10px 12px;
      background: #ffffff;
      border-top: 1px solid #e2e8f0;
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
