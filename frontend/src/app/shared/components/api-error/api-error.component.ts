import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValidationError } from '../../../core/models/qrbill.model';

@Component({
  selector: 'app-api-error',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="errors && errors.length > 0" class="error-container">
      <div class="error-header">
        <span class="error-icon">⚠️</span>
        <strong>Validation / API Errors:</strong>
      </div>
      <ul class="error-list">
        <li *ngFor="let err of errors">
          <span class="field-name">{{ formatField(err.field) }}:</span> {{ err.message }}
        </li>
      </ul>
    </div>
  `,
  styles: [`
    .error-container {
      background-color: rgba(244, 63, 94, 0.05);
      border: 1px solid rgba(244, 63, 94, 0.35);
      box-shadow: 0 4px 20px rgba(244, 63, 94, 0.12);
      border-radius: 12px;
      padding: 1.25rem;
      margin-bottom: 1.75rem;
      color: #f8fafc;
      animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    @keyframes slideIn {
      from { opacity: 0; transform: translateY(-8px); }
      to { opacity: 1; transform: translateY(0); }
    }
    .error-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.65rem;
      color: #f43f5e;
      font-size: 0.95rem;
    }
    .error-list {
      margin-left: 1.5rem;
      padding-left: 0;
    }
    .field-name {
      font-weight: 600;
      color: #94a3b8;
      text-transform: capitalize;
    }
  `]
})
export class ApiErrorComponent {
  @Input() errors: ValidationError[] = [];

  formatField(field: string): string {
    if (!field) return 'Error';
    return field
      .replace(/\./g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .trim();
  }
}
