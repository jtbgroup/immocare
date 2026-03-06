import { Component, Input, Output, EventEmitter, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TransactionAssetLink, AssetType } from '../../../../models/transaction.model';

@Component({
  selector: 'app-asset-link-editor',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './asset-link-editor.component.html',
  styleUrls: ['./asset-link-editor.component.scss']
})
export class AssetLinkEditorComponent implements OnChanges {
  @Input() buildingId: number | null = null;
  @Input() unitId: number | null = null;
  @Input() links: TransactionAssetLink[] = [];
  @Output() linksChanged = new EventEmitter<TransactionAssetLink[]>();

  localLinks: TransactionAssetLink[] = [];
  readonly assetTypes: AssetType[] = ['BOILER', 'FIRE_EXTINGUISHER', 'METER'];

  ngOnChanges(): void {
    this.localLinks = [...(this.links || [])];
  }

  addLink(): void {
    this.localLinks = [...this.localLinks, { assetType: 'BOILER', assetId: 0 }];
    this.emit();
  }

  removeLink(index: number): void {
    this.localLinks = this.localLinks.filter((_, i) => i !== index);
    this.emit();
  }

  onLinkChange(): void {
    this.emit();
  }

  private emit(): void {
    this.linksChanged.emit([...this.localLinks]);
  }

  get hasContext(): boolean {
    return this.buildingId != null || this.unitId != null;
  }
}
