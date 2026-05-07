import { Component, ChangeDetectionStrategy } from '@angular/core';
import { FormsModule } from '@angular/forms';

export interface AppConfig {
  appName: string;
  adminEmail: string;
  defaultLanguage: string;
  itemsPerPage: number;
  enableUserRegistration: boolean;
  apiBaseUrl: string;
  cloudinaryCloudName: string;
}

@Component({
  selector: 'app-config',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './config.html',
  styleUrl: './config.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Config {
  // Objeto de configuración que enlaza con los [(ngModel)] de la vista
  // Se inicializa con valores por defecto para evitar errores de nulidad
  config: AppConfig = {
    appName: 'Paw Portal Admin',
    adminEmail: 'admin@pawportal.org',
    defaultLanguage: 'es',
    itemsPerPage: 10,
    enableUserRegistration: true,
    apiBaseUrl: 'http://localhost:8080/api',
    cloudinaryCloudName: ''
  };

  /**
   * Método para guardar los cambios realizados en la configuración.
   */
  saveConfig(): void {
    console.log('Enviando cambios de configuración al servidor...', this.config);
    // Aquí integrarías la llamada a tu API o Servicio de configuración.
  }
}