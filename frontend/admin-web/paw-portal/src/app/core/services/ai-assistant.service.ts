import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';

export interface AiChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiNavigationAction {
  label: string;
  route: string;
}

export interface AiChatResponse {
  reply: string;
  actions: AiNavigationAction[];
  provider: string;
  model: string;
}

@Injectable({
  providedIn: 'root'
})
export class AiAssistantService {
  private http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/ai/chat`;

  sendMessage(message: string, history: AiChatMessage[]): Observable<AiChatResponse> {
    return this.http.post<AiChatResponse>(this.apiUrl, { message, history });
  }
}
